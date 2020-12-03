package org.checkerframework.checker.unconnectedsocket;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.unconnectedsocket.qual.PossiblyConnected;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;

public class UnconnectedSocketAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  final AnnotationMirror TOP;

  public UnconnectedSocketAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, PossiblyConnected.class);
    this.postInit();
  }
}
