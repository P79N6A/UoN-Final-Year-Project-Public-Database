package org.pmiops.workbench.cohortbuilder.querybuilder.util;

import org.apache.commons.lang3.math.NumberUtils;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Operator;

import java.util.function.Predicate;
import java.util.stream.Collectors;


public class AttributePredicates {

  public static Predicate<Attribute> categoricalAndNotIn() {
    return a -> AttrName.CAT.equals(a.getName()) &&
      !a.getOperator().equals(Operator.IN);
  }

  public static Predicate<Attribute> betweenOperator() {
    return a -> Operator.BETWEEN.equals(a.getOperator());
  }

  public static Predicate<Attribute> notBetweenOperator() {
    return a -> !Operator.BETWEEN.equals(a.getOperator());
  }

  public static Predicate<Attribute> operandsNotTwo() {
    return a -> a.getOperands().size() != 2;
  }

  public static Predicate<Attribute> operandsNotOne() {
    return a -> a.getOperands().size() != 1;
  }

  public static Predicate<Attribute> operatorNull() {
    return a -> a.getOperator() == null;
  }

  public static Predicate<Attribute> operandsEmpty() {
    return a -> a.getOperands().isEmpty();
  }

  public static Predicate<Attribute> nameBlank() {
    return a -> a.getName() == null;
  }

  public static Predicate<Attribute> anyAttr() {
    return a -> AttrName.ANY.equals(a.getName());
  }

  public static Predicate<Attribute> conceptIdIsNull() {
    return a -> a.getConceptId() == null;
  }

  public static Predicate<Attribute> operandsNotNumbers() {
    return a -> !a
      .getOperands()
      .stream()
      .filter(o -> !NumberUtils.isNumber(o))
      .collect(Collectors.toList()).isEmpty();
  }
}
