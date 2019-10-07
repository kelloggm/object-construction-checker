package org.checkerframework.checker.objectconstruction.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier in the Called Methods type hierarchy. It is equivalent to @CalledMethods({}).
 *
 * <p>It is needed so that @CalledMethodsPredicate can be a sibling of @CalledMethods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultQualifierInHierarchy
@SubtypeOf({})
public @interface CalledMethodsTop {}
