package org.checkerframework.checker.mustcall.qual;

import org.checkerframework.framework.qual.JavaExpression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the method resets the must-call obligation on the target (the value argument) to its declaration when
 * it is invoked. Calling a method with this annotation also clears the called-methods
 * store for the target (in the Object Construction Checker).
 *
 * The default target is "this".
 *
 * It is an error to call a method annotated by this annotation if the target object is
 * declared as having a non-empty CalledMethods type (i.e. its type is not top in the CM hierarchy).
 *
 * It is an error to fail to write this annotation on any method that (re-)assigns a non-final,
 * owning field with a must-call obligation. It is an error if the target of that annotation is not
 * exactly the value "this".
 *
 * This annotation is trusted, not checked (though it can only add obligations, so is still conservative).
 */
@Target({ElementType.METHOD})
public @interface ResetMustCall {

  /** @return the object to which must-call obligations are added when the annotated method is invoked */
  @JavaExpression
  String value() default "this";
}
