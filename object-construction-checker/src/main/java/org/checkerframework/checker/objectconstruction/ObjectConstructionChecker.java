package org.checkerframework.checker.objectconstruction;

import static javax.tools.Diagnostic.Kind.WARNING;
import static org.checkerframework.checker.mustcall.MustCallChecker.NO_ACCUMULATION_FRAMES;
import static org.checkerframework.checker.mustcall.MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP;
import static org.checkerframework.checker.mustcall.MustCallChecker.NO_RESOURCE_ALIASES;
import static org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.CHECK_MUST_CALL;
import static org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.COUNT_MUST_CALL;

import java.util.LinkedHashSet;
import java.util.Properties;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoAccumulationFramesChecker;
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
  "NoObligationGenerics.astub",
  "NoObligationStreams.astub",
  "IOUtils.astub",
  "Reflection.astub",
})
@SupportedOptions({
  CHECK_MUST_CALL,
  COUNT_MUST_CALL,
  NO_ACCUMULATION_FRAMES,
  NO_LIGHTWEIGHT_OWNERSHIP,
  NO_RESOURCE_ALIASES
})
public class ObjectConstructionChecker extends CalledMethodsChecker {

  public static final String CHECK_MUST_CALL = "checkMustCall";

  public static final String COUNT_MUST_CALL = "countMustCall";

  /**
   * The number of expressions with must-call obligations that were checked. Incremented only if the
   * {@link #COUNT_MUST_CALL} option was supplied.
   */
  int numMustCall = 0;

  int numMustCallFailed = 0;

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();

    if (this.processingEnv.getOptions().containsKey(CHECK_MUST_CALL)) {
      if (this.processingEnv.getOptions().containsKey(NO_ACCUMULATION_FRAMES)) {
        checkers.add(MustCallNoAccumulationFramesChecker.class);
      } else {
        checkers.add(MustCallChecker.class);
      }
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
        "@MustCall %s not invoked.  The type of object is: %s.  Reason for going out of scope: %s\n");
    messages.setProperty(
        "missing.create.obligation",
        "This method re-assigns the non-final, owning field %s.%s, but does not have a corresponding @CreateObligation annotation.\n");
    messages.setProperty(
        "incompatible.create.obligation",
        "This method re-assigns the non-final, owning field %s.%s, but its @CreateObligation annotation targets %s.\n");
    messages.setProperty(
        "reset.not.owning",
        "Calling this method resets the must-call obligations of the expression %s, which is non-owning. Either annotate its declaration with an @Owning annotation or write a corresponding @CreateObligation annotation on the method that encloses this statement.\n");
    return messages;
  }

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new ObjectConstructionVisitor(this);
  }

  @Override
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    if (messageKey.equals("required.method.not.called")) {
      // This looks crazy but it's safe because of the warning key.
      String qualifiedTypeName = (String) args[1];
      if (qualifiedTypeName.startsWith("java")) {
        numMustCallFailed++;
      }
    }
    super.reportError(source, messageKey, args);
  }

  @Override
  public void typeProcessingOver() {
    if (hasOption(COUNT_MUST_CALL)) {
      message(WARNING, "Found %d must call obligation(s).%n", numMustCall);
      message(
          WARNING,
          "Successfully verified %d must call obligation(s).%n",
          numMustCall - numMustCallFailed);
    }
    super.typeProcessingOver();
  }
}
