package org.checkerframework.checker.objectconstruction;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import org.checkerframework.checker.returnsrcvr.ReturnsRcvrChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.Result;
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

    // BaseTypeChecker#hasOption calls this method (so that all subcheckers' options are
    // considered),
    // so the processingEnvironment must be checked for the option directly.
    if (this.processingEnv.getOptions().containsKey(USE_VALUE_CHECKER)) {
      checkers.add(ValueChecker.class);
    }
    return checkers;
  }

  /**
   * Parses the String representation of an @CalledMethods annotation into a Set containing all of
   * the actual arguments to the annotation.
   *
   * <p>Also parses @CalledMethodsTop into the empty set.
   */
  private Set<String> parseCalledMethods(String calledMethodsString) {
    if (calledMethodsString.contains("@CalledMethodsTop")) {
      return Collections.emptySet();
    }

    String withoutCMAnno = calledMethodsString.trim().substring("@CalledMethods({".length());
    String withoutBaseType = withoutCMAnno.substring(0, withoutCMAnno.lastIndexOf(' '));
    if (withoutBaseType.length() >= 2) {
      return new HashSet<>(
          Arrays.asList(withoutBaseType.substring(0, withoutBaseType.length() - 2).split(", ")));
    } else {
      return Collections.emptySet();
    }
  }

  /**
   * Adds special reporting for method.invocation.invalid errors to turn them into
   * finalizer.invocation.invalid errors.
   */
  @Override
  public void report(final Result r, final Object src) {

    Result theResult = r;

    String errKey = r.getMessageKeys().iterator().next();
    if ("method.invocation.invalid".equals(errKey) && r.isFailure()) {
      Object[] args = r.getDiagMessages().iterator().next().getArgs();
      String actualReceiverAnnoString = (String) args[1];
      String requiredReceiverAnnoString = (String) args[2];
      if (actualReceiverAnnoString.contains("@CalledMethods(")
          || actualReceiverAnnoString.contains("@CalledMethodsTop")) {
        Set<String> actualCalledMethods = parseCalledMethods(actualReceiverAnnoString);
        if (requiredReceiverAnnoString.contains("@CalledMethods(")) {
          Set<String> requiredCalledMethods = parseCalledMethods(requiredReceiverAnnoString);
          requiredCalledMethods.removeAll(actualCalledMethods);
          StringBuilder missingMethods = new StringBuilder();
          for (String s : requiredCalledMethods) {
            if (s.startsWith("\"")) {
              s = s.substring(1);
            }
            if (s.endsWith("\"")) {
              s = s.substring(0, s.length() - 1);
            }
            missingMethods.append(s);
            missingMethods.append("() ");
          }
          theResult = Result.failure("finalizer.invocation.invalid", missingMethods.toString());
        }
      }
    }
    super.report(theResult, src);
  }

  /**
   * Overridden because the messages.properties file isn't being loaded, for some reason. I think it
   * has to do with relative paths? For whatever reason, this has to be hardcoded into the checker
   * itself here for checkers that aren't part of the CF itself.
   */
  @Override
  public Properties getMessages() {
    Properties messages = super.getMessages();
    messages.setProperty(
        "finalizer.invocation.invalid",
        "This finalizer cannot be invoked, because the following methods have not been called: %s\n");
    messages.setProperty(
        "predicate.invalid",
        "An unparseable predicate was found in an annotation. Predicates must be produced by this grammar: S → method name | (S) | S && S | S || S. The message from the evaluator was: %s \\n");
    return messages;
  }
}
