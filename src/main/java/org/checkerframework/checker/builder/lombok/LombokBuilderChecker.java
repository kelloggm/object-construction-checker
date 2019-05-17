package org.checkerframework.checker.builder.lombok;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A subchecker of the typesafe builder checker that places annotations correctly for Lombok
 * builders. It runs before the main CalledMethods Checker.
 */
public class LombokBuilderChecker extends BaseTypeChecker {}
