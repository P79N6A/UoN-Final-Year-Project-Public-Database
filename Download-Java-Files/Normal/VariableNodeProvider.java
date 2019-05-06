/*******************************************************************************
 * Copyright (c) 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.designer.internal.ui.views.data.providers;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.internal.ui.views.DefaultNodeProvider;
import org.eclipse.birt.report.designer.internal.ui.views.actions.EditAction;
import org.eclipse.birt.report.designer.nls.Messages;
import org.eclipse.birt.report.designer.ui.dialogs.VariableDialog;
import org.eclipse.birt.report.model.api.ContentElementHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.VariableElementHandle;
import org.eclipse.birt.report.model.elements.interfaces.IReportDesignModel;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * 
 */

public class VariableNodeProvider extends DefaultNodeProvider
{

	@Override
	public Object getParent( Object model )
	{
		if ( model != null
				&& ( (VariableElementHandle) model ).getRoot( ) != null )
			return ( (VariableElementHandle) model ).getRoot( )
					.getPropertyHandle( IReportDesignModel.PAGE_VARIABLES_PROP );
		return null;
	}

	public Object[] getChildren( Object model )
	{
		return new Object[0];
	}

	/**
	 * Creates the context menu for the given object. Gets the action from the
	 * actionRegistry for the given object and adds them to the menu
	 * 
	 * @param menu
	 *            the menu
	 * @param object
	 *            the object
	 */
	public void createContextMenu( TreeViewer sourceViewer, Object object,
			IMenuManager menu )
	{
		super.createContextMenu( sourceViewer, object, menu );
		// menu.insertAfter( IWorkbenchActionConstants.MB_ADDITIONS,
		// new EditVariableAction( object ) );
		menu.insertAfter( IWorkbenchActionConstants.MB_ADDITIONS,
				new EditAction( object,
						Messages.getString( "ParameterNodeProvider.menu.text.edit" ) ) );
	}

	public String getNodeDisplayName( Object object )
	{
		return ( (VariableElementHandle) object ).getDisplayLabel( );
	}

	protected boolean performEdit( ContentElementHandle handle )
	{
		VariableDialog dialog = new VariableDialog( Messages.getString( "VariableNodeProvider.DialogTitle" ), //$NON-NLS-1$
				(ReportDesignHandle) SessionHandleAdapter.getInstance( )
						.getReportDesignHandle( ),
				(VariableElementHandle) handle );
		dialog.open( );
		return true;
	}

}
