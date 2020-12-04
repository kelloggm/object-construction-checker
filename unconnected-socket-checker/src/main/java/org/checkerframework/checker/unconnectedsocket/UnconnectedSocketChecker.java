package org.checkerframework.checker.unconnectedsocket;

import java.util.Properties;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * This simple typechecker tracks which sockets are definitely unconnected, and therefore don't have
 * to be closed.
 */
@StubFiles({"Sockets.astub"})
public class UnconnectedSocketChecker extends BaseTypeChecker {
  /**
   * Overridden because the messages.properties file isn't being loaded, for some reason. I think it
   * has to do with relative paths? For whatever reason, this has to be hardcoded into the checker
   * itself here for checkers that aren't part of the CF itself.
   */
  @Override
  public Properties getMessagesProperties() {
    Properties messages = super.getMessagesProperties();
    messages.setProperty(
        "unconnected.field",
        "This checker must treat all fields as @PossiblyConnected, for soundness. "
            + "Remove any annotations you have written on this field.");
    return messages;
  }
}
