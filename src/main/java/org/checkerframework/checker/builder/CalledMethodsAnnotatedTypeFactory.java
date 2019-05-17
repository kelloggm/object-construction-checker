package org.checkerframework.checker.builder;

import javax.lang.model.element.AnnotationMirror;

public interface CalledMethodsAnnotatedTypeFactory {
  AnnotationMirror createCalledMethods(String... val);
}
