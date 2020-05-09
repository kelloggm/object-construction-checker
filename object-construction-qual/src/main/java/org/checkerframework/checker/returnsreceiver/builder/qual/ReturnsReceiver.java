package org.checkerframework.checker.returnsreceiver.builder.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This declaration annotation indicates that the method on which it is written returns exactly the
 * receiver object. should not be used in new code, because it is TRUSTED, NOT CHECKED.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ReturnsReceiver {}
