package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * If an expression has type {@code @MustCall({"m1", "m2"})}, then its value might have an
 * obligation to eventually call methods {@code m1}, {@code m2}, both, or neither before any exit
 * point or de-allocation. The value cannot have an obligation to call any methods that are not
 * named in the annotation; that is, the methods listed in the annotation are an over-approximation
 * of the actual methods that need to be called.
 *
 * <p>The subtyping relationship is:
 *
 * <pre>{@code @MustCall({"m1"}) <: @MustCall({"m1", "m2"})}</pre>
 *
 * <p>For example, an object with an obligation to call "m1" can be passed to a method whose
 * argument has an obligation to call both "m1" and "m2", because if both "m1" and "m2" are eventually called,
 * then "m1" must also have been called.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({MustCallAll.class})
@DefaultQualifierInHierarchy
@DefaultFor({TypeUseLocation.EXCEPTION_PARAMETER})
public @interface MustCall {
  /**
   * Methods that must be called, on any expression whose type is annotated.
   *
   * @return methods that must be called
   */
  public String[] value() default {};
}
