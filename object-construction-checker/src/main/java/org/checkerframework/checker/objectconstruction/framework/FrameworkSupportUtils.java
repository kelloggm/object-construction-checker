package org.checkerframework.checker.objectconstruction.framework;

import java.lang.annotation.Annotation;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

public class FrameworkSupportUtils {
  // this class is non-instantiable
  private FrameworkSupportUtils() {}

  public static boolean isGuavaImmutableType(TypeMirror returnType) {
    return returnType.toString().startsWith("com.google.common.collect.Immutable");
  }

  public static String capitalize(String prop) {
    return prop.substring(0, 1).toUpperCase() + prop.substring(1);
  }

  /**
   * Given an annotation class, return true if the element has the annotation
   *
   * @param element the element
   * @param annotClass class for the annotation
   * @return true if the element has the annotation
   */
  public static boolean hasAnnotation(Element element, Class<? extends Annotation> annotClass) {
    return element.getAnnotation(annotClass) != null;
  }

  /**
   * Given an annotation name, return true if the element has the annotation of that name
   *
   * @param element the element
   * @param annotName name of the annotation
   * @return true if the element has the annotation of that name
   */
  public static boolean hasAnnotation(Element element, String annotName) {
    return element.getAnnotationMirrors().stream()
        .anyMatch(anm -> AnnotationUtils.areSameByName(anm, annotName));
  }
}
