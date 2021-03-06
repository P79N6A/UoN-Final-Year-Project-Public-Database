package com.bakdata.conquery.models.concepts.conditions;

import java.io.Serializable;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
/**
 * A general condition that serves as a guard for concept tree nodes.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface CTCondition extends Serializable {

	public default void init(ConceptTreeNode node) throws ConceptConfigurationException {}
	
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException;

}
