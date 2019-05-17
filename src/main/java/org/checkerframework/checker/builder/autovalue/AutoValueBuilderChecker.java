package org.checkerframework.checker.builder.autovalue;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A subchecker of the typesafe builder checker that places annotations correctly for AutoValue
 * builders. It runs before the main CalledMethods Checker.
 */
public class AutoValueBuilderChecker extends BaseTypeChecker {}
