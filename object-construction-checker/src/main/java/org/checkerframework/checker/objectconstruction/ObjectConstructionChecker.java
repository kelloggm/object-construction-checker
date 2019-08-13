package org.checkerframework.checker.objectconstruction;

import java.util.LinkedHashSet;
import org.checkerframework.checker.returnsrcvr.ReturnsRcvrChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * The primary typechecker for the object construction checker, which allows programmers to specify
 * unsafe combinations of options to builder or builder-like interfaces and prevent dangerous
 * objects from being instantiated.
 */
@SuppressWarningsKeys({"builder", "object.construction", "objectconstruction"})
@SupportedOptions(ObjectConstructionChecker.USE_VALUE_CHECKER)
public class ObjectConstructionChecker extends BaseTypeChecker {

  public static final String USE_VALUE_CHECKER = "useValueChecker";

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();
    checkers.add(ReturnsRcvrChecker.class);
    if (this.processingEnv.getOptions().containsKey(USE_VALUE_CHECKER)) {
      checkers.add(ValueChecker.class);
    }
    return checkers;
  }
}
