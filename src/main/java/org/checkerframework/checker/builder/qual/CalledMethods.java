package org.checkerframework.checker.builder.qual;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation keeps track of the methods called on an object.
 * It mostly forms its own lattice: @CalledMethods({}) is top,
 * and @CalledMethods(A) is a subtype of @CalledMethod(B) iff B is a subset of a A.
 *
 * There is also a bottom annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface CalledMethods {
    /** A list of methods that have been called on the annotated object. */
    public String[] value() default {};
}
