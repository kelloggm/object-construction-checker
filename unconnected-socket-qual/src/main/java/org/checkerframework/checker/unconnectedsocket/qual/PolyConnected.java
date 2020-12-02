package org.checkerframework.checker.unconnectedsocket.qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A standard polymorphic qualifier for the connectedness type system.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(PossiblyConnected.class)
public @interface PolyConnected {
}
