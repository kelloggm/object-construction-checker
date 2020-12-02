package org.checkerframework.checker.unconnectedsocket;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * This simple typechecker tracks which sockets are definitely unconnected, and therefore don't have
 * to be closed.
 */
public class UnconnectedSocketChecker extends BaseTypeChecker {}
