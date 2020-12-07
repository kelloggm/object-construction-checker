package org.checkerframework.checker.unconnectedsocket.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This declaration annotation indicates that calling the annotated method cannot cause a socket to
 * be connected.
 *
 * <p>This annotation is TRUSTED, NOT CHECKED. Usually, it is only written in stub files.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CannotConnect {}
