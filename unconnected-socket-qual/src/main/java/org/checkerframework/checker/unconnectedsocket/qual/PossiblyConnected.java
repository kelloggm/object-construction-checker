package org.checkerframework.checker.unconnectedsocket.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The top qualifier in the Unconnected Socket checker hierarchy. It can represent any expression,
 * but is named "possibly connected" in contrast to {@link Unconnected}, which means "definitely
 * unconnected".
 *
 * <p>An expression with this type will have its must-call obligations checked as normal.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultQualifierInHierarchy
@DefaultFor(TypeUseLocation.FIELD)
@SubtypeOf({})
public @interface PossiblyConnected {}
