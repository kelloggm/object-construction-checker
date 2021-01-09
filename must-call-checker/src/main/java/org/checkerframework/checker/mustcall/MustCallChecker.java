package org.checkerframework.checker.mustcall;

import java.util.Properties;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * This typechecker ensures that {@link MustCall} annotations are consistent with one another. The
 * Object Construction Checker verifies that the given methods are actually called.
 */
@StubFiles({"Socket.astub", "NotOwning.astub", "Stream.astub", "NoObligationStreams.astub"})
public class MustCallChecker extends BaseTypeChecker {

  /**
   * Overridden because the messages.properties file isn't being loaded, for some reason. I think it
   * has to do with relative paths? For whatever reason, this has to be hardcoded into the checker
   * itself here for checkers that aren't part of the CF itself.
   */
  @Override
  public Properties getMessagesProperties() {
    Properties messages = super.getMessagesProperties();
    messages.setProperty(
        "inconsistent.mustcall.subtype",
        "%s is annotated as %s, but one of its supertypes has an %s annotation, which is inconsistent.");
    messages.setProperty(
            "mustcall.not.parseable",
            "The method %s is annotated as @ResetMustCall, but the target (%s) was unparseable in the current context. " +
                    "Rewrite your code so that the relevant expression is a local variable.");
    return messages;
  }
}
