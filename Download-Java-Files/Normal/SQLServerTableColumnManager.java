/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataType;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerObjectClass;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableBase;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * SQLServer table column manager
 */
public class SQLServerTableColumnManager extends SQLTableColumnManager<SQLServerTableColumn, SQLServerTableBase> implements DBEObjectRenamer<SQLServerTableColumn> {

    protected final ColumnModifier<SQLServerTableColumn> IdentityModifier = (monitor, column, sql, command) -> {
        if (column.isAutoGenerated()) {
            try {
                SQLServerTableColumn.IdentityInfo identityInfo = column.getIdentityInfo(monitor);
                sql.append(" IDENTITY(").append(identityInfo.getSeedValue()).append(",").append(identityInfo.getIncrementValue()).append(")");
            } catch (DBCException e) {
                log.error("Error reading identity information", e);
            }
        }
    };

    protected final ColumnModifier<SQLServerTableColumn> CollateModifier = (monitor, column, sql, command) -> {
        String collationName = column.getCollationName();
        if (!CommonUtils.isEmpty(collationName)) {
            sql.append(" COLLATE ").append(collationName);
        }
    };

    protected final ColumnModifier<SQLServerTableColumn> SQLServerDefaultModifier = (monitor, column, sql, command) -> {
        if (!column.isPersisted()) {
            DefaultModifier.appendModifier(monitor, column, sql, command);
        } else {
            // Modify existing column
            // TODO: implement default constraint create/drop
            DefaultModifier.appendModifier(monitor, column, sql, command);
        }
    };

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerTableColumn> getObjectsCache(SQLServerTableColumn object)
    {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache(object.getParentObject());
    }

    protected ColumnModifier[] getSupportedModifiers(SQLServerTableColumn column, Map<String, Object> options)
    {
        return new ColumnModifier[] {DataTypeModifier, IdentityModifier, CollateModifier, SQLServerDefaultModifier, NullNotNullModifier};
    }

    @Override
    protected SQLServerTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, SQLServerTableBase parent, Object copyFrom)
    {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar"); //$NON-NLS-1$

        final SQLServerTableColumn column = new SQLServerTableColumn(parent);
        column.setName(getNewColumnName(monitor, context, parent));
        column.setDataType((SQLServerDataType) columnType);
        column.setTypeName(columnType == null ? "varchar" : columnType.getName()); //$NON-NLS-1$
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.VARCHAR : columnType.getTypeID());
        column.setOrdinalPosition(-1);
        return column;
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        final SQLServerTableColumn column = command.getObject();
        boolean hasComment = command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null;
        if (!hasComment || command.getProperties().size() > 1) {
            actionList.add(new SQLDatabasePersistAction(
                "Modify column",
                "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-1$
                " ALTER COLUMN " + getNestedDeclaration(monitor, column.getTable(), command, options))); //$NON-NLS-1$
        }
        if (hasComment) {
            boolean isUpdate = SQLServerUtils.isCommentSet(
                monitor,
                column.getTable().getDatabase(),
                SQLServerObjectClass.OBJECT_OR_COLUMN,
                column.getTable().getObjectId(),
                column.getObjectId());
            actionList.add(
                new SQLDatabasePersistAction(
                    "Add column comment",
                    "EXEC " + SQLServerUtils.getSystemTableName(column.getTable().getDatabase(), isUpdate ? "sp_updateextendedproperty" : "sp_addextendedproperty") +
                        " 'MS_Description', " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription()) + "," +
                        " 'user', '" + column.getTable().getSchema().getName() + "'," +
                        " 'table', '" + column.getTable().getName() + "'," +
                        " 'column', '" + column.getName() + "'"));
        }
    }

    @Override
    public void renameObject(DBECommandContext commandContext, SQLServerTableColumn object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final SQLServerTableColumn column = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                    "EXEC " + SQLServerUtils.getSystemTableName(column.getTable().getDatabase(), "sp_rename") +
                    " '" + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DML) + "." + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) +
                    "' , '" + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName()) + "', 'COLUMN'")
        );
    }

}
