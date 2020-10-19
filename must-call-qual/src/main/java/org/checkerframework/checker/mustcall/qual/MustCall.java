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
 * If an expression has type {@code @MustCall({"m1", "m2"})}, then its value has an obligation to
 * eventually call methods {@code m1} and {@code m2} before any exit point or de-allocation.
 *
 * <p>The subtyping relationship is:
 *
 * <pre>{@code @MustCall({"m1", "m2"}) <: @MustCall({"m1"})}</pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({MustCallTop.class})
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
