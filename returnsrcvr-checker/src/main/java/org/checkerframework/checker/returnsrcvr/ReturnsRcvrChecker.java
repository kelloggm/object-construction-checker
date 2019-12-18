package org.checkerframework.checker.returnsrcvr;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Empty Checker is the entry point for pluggable type-checking.
 *
 * <p>This one does nothing. The Checker Framework manual tells you how to make it do something:
 * https://checkerframework.org/manual/#creating-a-checker
 */
public class ReturnsRcvrChecker extends BaseTypeChecker {
	public static final String UNABLE_FRAMEWORK_SUPPORTS = "unableFrameworkSupports";
	public static final String LOMBOK_SUPPORT = "Lombok";
	public static final String AUTOVALUE_SUPPORT = "Autovalue";
	  
}
