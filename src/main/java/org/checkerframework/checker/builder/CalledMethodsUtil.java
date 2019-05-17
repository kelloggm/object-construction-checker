package org.checkerframework.checker.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.builder.qual.CalledMethods;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

public class CalledMethodsUtil {

  public static AnnotationMirror createCalledMethodsImpl(
      AnnotationMirror top, ProcessingEnvironment processingEnv, String... val) {
    if (val.length == 0) {
      return top;
    }
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, CalledMethods.class);
    Arrays.sort(val);
    builder.setValue("value", val);
    return builder.build();
  }

  /**
   * Gets the value field of an annotation with a list of strings in its value field. The empty list
   * is returned if no value field is defined.
   */
  public static List<String> getValueOfAnnotationWithStringArgument(final AnnotationMirror anno) {
    if (!AnnotationUtils.hasElementValue(anno, "value")) {
      return Collections.emptyList();
    }
    return AnnotationUtils.getElementValueArray(anno, "value", String.class, true);
  }
}
