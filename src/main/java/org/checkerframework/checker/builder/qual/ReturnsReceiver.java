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
 * <p>The annotation is used by the Typesafe Builder Checker to determine whether to
 * copy @CalledMethods annotations from one invocation of a builder's methods to others in a fluent
 * builder chain.
 *
 * <p>This annotation can only be written on a method declaration. It is inherited by all overrides
 * of that method.
 *
 * <p>TODO: describe how this is checked, or whether it is trusted
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ReturnsReceiver {}
