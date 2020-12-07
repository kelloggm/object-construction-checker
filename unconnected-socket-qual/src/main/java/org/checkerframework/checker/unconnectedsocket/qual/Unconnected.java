package org.checkerframework.checker.unconnectedsocket.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If an expression has type {@code {@literal @}Unconnected}, then it definitely represents an
 * unconnected socket - that is, a socket that does not yet have a file descriptor assigned to it.
 * For example, the expression {@code new java.net.Socket()} has the type {@code
 * {@literal @}Unconnected Socket}. Unconnected sockets are not obligated to call their "close"
 * methods before deallocation.
 *
 * <p>This type can also be used to represent any other expression that definitely does not need to
 * fulfill its must-call obligations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({PossiblyConnected.class})
public @interface Unconnected {}
