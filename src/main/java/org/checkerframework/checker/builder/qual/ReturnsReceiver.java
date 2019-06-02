package org.checkerframework.checker.builder.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This declaration annotation indicates that the method on which it is written returns exactly the
 * receiver object.
 *
 * <p>This annotation can only be written on a method declaration. It is inherited by all overrides
 * of that method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ReturnsReceiver {}
