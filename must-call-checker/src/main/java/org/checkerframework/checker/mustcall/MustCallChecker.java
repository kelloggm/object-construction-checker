package org.checkerframework.checker.mustcall;

import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * This typechecker is responsible for propagating {@link MustCall} annotations in support of the
 * Object Construction Checker's ability to check must-call obligations.
 */
@StubFiles({"Socket.astub", "NotOwning.astub", "Stream.astub", "NoObligationStreams.astub"})
public class MustCallChecker extends BaseTypeChecker {}
