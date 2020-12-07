package org.checkerframework.checker.objectconstruction;

import static org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.CHECK_MUST_CALL;
import static org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.COUNT_MUST_CALL;

import java.util.LinkedHashSet;
import java.util.Properties;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * The primary typechecker for the object construction checker, which allows programmers to specify
 * unsafe combinations of options to builder or builder-like interfaces and prevent dangerous
 * objects from being instantiated.
 */
@SuppressWarningsPrefix({"builder", "object.construction", "objectconstruction"})
@StubFiles({
  "Socket.astub",
  "NotOwning.astub",
  "Stream.astub",
  "NoObligationStreams.astub",
  "IOUtils.astub"
})
@SupportedOptions({CHECK_MUST_CALL, COUNT_MUST_CALL})
public class ObjectConstructionChecker extends CalledMethodsChecker {

  public static final String CHECK_MUST_CALL = "checkMustCall";

  public static final String COUNT_MUST_CALL = "countMustCall";

  /**
   * The number of expressions with must-call obligations that were checked. Incremented only if the
   * {@link #COUNT_MUST_CALL} option was supplied.
   */
  int numMustCall = 0;

  int numMustCallPassed = 0;

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();

    if (this.processingEnv.getOptions().containsKey(CHECK_MUST_CALL)) {
      checkers.add(MustCallChecker.class);
    }

    return checkers;
  }

  /**
   * Overridden because the messages.properties file isn't being loaded, for some reason. I think it
   * has to do with relative paths? For whatever reason, this has to be hardcoded into the checker
   * itself here for checkers that aren't part of the CF itself.
   */
  @Override
  public Properties getMessagesProperties() {
    Properties messages = super.getMessagesProperties();
    messages.setProperty(
        "ensuresvarargs.annotation.invalid",
        "@EnsuresCalledMethodsVarArgs cannot be written on a non-varargs method");
    messages.setProperty(
        "ensuresvarargs.unverified",
        "@EnsuresCalledMethodsVarArgs cannot be verified yet.  Please check that the implementation of the method actually does call the given methods on the varargs parameters by hand, and then suppress the warning.");
    messages.setProperty(
        "required.method.not.called",
        "@MustCall method(s) %s for variable/expression not invoked.  The type of object is: %s.  Reason for going out of scope: %s\n");
    return messages;
  }

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new ObjectConstructionVisitor(this);
  }

  @Override
  public void typeProcessingOver() {
    if (hasOption(COUNT_MUST_CALL)) {
      System.out.printf("Found %d must call obligation(s).%n", numMustCall);
      System.out.printf(
          "Found %d must call obligation(s) that were handled correctly.%n", numMustCallPassed);
    }
    super.typeProcessingOver();
  }
}
