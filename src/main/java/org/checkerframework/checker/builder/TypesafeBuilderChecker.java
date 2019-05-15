package org.checkerframework.checker.builder;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * The primary typechecker for the typesafe builder checker, which allows programmers to specify
 * unsafe combinations of options to builder or builder-like interfaces and prevent dangerous
 * objects from being instantiated.
 */
@SuppressWarningsKeys({"builder", "typesafe.builder"})
public class TypesafeBuilderChecker extends BaseTypeChecker {}
