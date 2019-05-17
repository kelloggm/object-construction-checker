package org.checkerframework.checker.builder;

import java.util.LinkedHashSet;
import org.checkerframework.checker.builder.lombok.LombokBuilderChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * The primary typechecker for the typesafe builder checker, which allows programmers to specify
 * unsafe combinations of options to builder or builder-like interfaces and prevent dangerous
 * objects from being instantiated.
 */
@SuppressWarningsKeys({"builder", "typesafe.builder"})
public class TypesafeBuilderChecker extends BaseTypeChecker {
  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();
    checkers.add(LombokBuilderChecker.class);
    return checkers;
  }
}
