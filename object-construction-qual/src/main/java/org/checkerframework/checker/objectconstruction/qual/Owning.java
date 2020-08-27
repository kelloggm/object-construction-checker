package org.checkerframework.checker.objectconstruction.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//
//
//// @TargetLocations({TypeUseLocation.PARAMETER, TypeUseLocation.FIELD, TypeUseLocation.RETURN})
/**
 * Annotation indicating ownership should be transferred to the parameter or field, for the purposes
 * of AlwaysCall checking.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface Owning {}
