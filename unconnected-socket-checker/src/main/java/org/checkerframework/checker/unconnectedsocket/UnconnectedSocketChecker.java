package org.checkerframework.checker.unconnectedsocket;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * This simple typechecker tracks which sockets are definitely unconnected, and therefore don't have
 * to be closed.
 */
@StubFiles({"Sockets.astub"})
public class UnconnectedSocketChecker extends BaseTypeChecker {}
