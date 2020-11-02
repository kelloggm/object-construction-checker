package org.checkerframework.checker.mustcall;

import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * This typechecker ensures that {@link MustCall} annotations are consistent with one another. The
 * Object Construction Checker verifies that the given methods are actually called.
 */
@StubFiles({"Socket.astub", "NotOwning.astub", "Stream.astub", "NoObligationStreams.astub"})
public class MustCallChecker extends BaseTypeChecker {}
