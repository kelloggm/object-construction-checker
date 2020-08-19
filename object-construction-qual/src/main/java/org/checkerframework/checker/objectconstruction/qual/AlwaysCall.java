package org.checkerframework.checker.objectconstruction.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If an expression has type {@code @AlwaysCall({"m1", "m2"})}, then methods {@code m1} and {@code
 * m2} have definitely been called on its value before any exit point or de-allocation. Other
 * methods might or might not have been called.
 *
 * <p>The subtyping relationship is:
 *
 * <pre>{@code @AlwaysCall({"m1", "m2"}) <: @AlwaysCall({"m1", "m2", "m3"})}</pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AlwaysCall {
  /**
   * Methods that have been called, on any expression whose type is annotated.
   *
   * @return methods that have been called
   */
  public String value() default "";
}
