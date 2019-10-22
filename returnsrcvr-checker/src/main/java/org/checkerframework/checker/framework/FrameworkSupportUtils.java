package org.checkerframework.checker.framework;

import java.lang.annotation.Annotation;
import javax.lang.model.element.Element;
import org.checkerframework.javacutil.AnnotationUtils;

/** A utility class for framework support in returns receiver checker */
public class FrameworkSupportUtils {

  // this class is non-instantiable
  private FrameworkSupportUtils() {}

  /**
   * Given an annotation class, return true if the element has the annotation
   *
   * @param element
   * @param annotClass
   * @return true if the element has the annotation
   */
  public static boolean hasAnnotation(Element element, Class<? extends Annotation> annotClass) {
    return element.getAnnotationMirrors().stream()
        .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
  }

  /**
   * Given an annotation name, return true if the element has the annotation of that name
   *
   * @param element
   * @param annotClassName
   * @return true if the element has the annotation of that name
   */
  public static boolean hasAnnotationByName(Element element, String annotClassName) {
    return element.getAnnotationMirrors().stream()
        .anyMatch(anm -> AnnotationUtils.areSameByName(anm, annotClassName));
  }
}
