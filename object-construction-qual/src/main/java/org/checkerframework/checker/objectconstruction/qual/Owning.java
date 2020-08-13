package org.checkerframework.checker.objectconstruction.qual;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Annotation indicating ownership should be transferred to the parameter or field, for the purposes
 * of AlwaysCall checking.
 */
@Retention(RetentionPolicy.RUNTIME)
@TargetLocations({TypeUseLocation.PARAMETER, TypeUseLocation.FIELD})
public @interface Owning {}
