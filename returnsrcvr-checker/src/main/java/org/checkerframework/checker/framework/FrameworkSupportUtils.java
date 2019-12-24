package org.checkerframework.checker.framework;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.checker.returnsrcvr.ReturnsRcvrChecker;
import org.checkerframework.javacutil.AnnotationUtils;

/** A utility class for framework support in returns receiver checker */
public class FrameworkSupportUtils {

  // this class is non-instantiable
  private FrameworkSupportUtils() {}

  public enum Framework {
    AUTO_VALUE,
    LOMBOK;
  }

  public static Set<Framework> getFrameworkSet(String option) {
    Set<Framework> frameworkSet = new HashSet<Framework>(Arrays.asList(Framework.values()));
    Set<Framework> disableFrameworkSet = new HashSet<Framework>();

    if (option != null) {
      for (String disabledFrameworkSupport : option.split("\\s?,\\s?")) {
        switch (disabledFrameworkSupport.toUpperCase()) {
          case ReturnsRcvrChecker.AUTOVALUE_SUPPORT:
            disableFrameworkSet.add(Framework.AUTO_VALUE);
            break;
          case ReturnsRcvrChecker.LOMBOK_SUPPORT:
            disableFrameworkSet.add(Framework.LOMBOK);
            break;
        }
      }
    }
    frameworkSet.removeAll(disableFrameworkSet);
    return frameworkSet;
  }

  /**
   * Given an annotation class, return true if the element has the annotation
   *
   * @param element the element that might have an annotation
   * @param annotClass the class of the annotation that might be present
   * @return true if the element has the annotation
   */
  public static boolean hasAnnotation(Element element, Class<? extends Annotation> annotClass) {
    return element.getAnnotationMirrors().stream()
        .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
  }

  /**
   * Given an annotation name, return true if the element has the annotation of that name
   *
   * @param element the element that might have an annotation
   * @param annotClassName the class of the annotation that might be present
   * @return true if the element has the annotation of that class
   */
  public static boolean hasAnnotationByName(Element element, String annotClassName) {
    return element.getAnnotationMirrors().stream()
        .anyMatch(anm -> AnnotationUtils.areSameByName(anm, annotClassName));
  }
}
