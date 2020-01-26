package org.checkerframework.checker.objectconstruction;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Collections;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.objectconstruction.framework.AutoValueSupport;
import org.checkerframework.checker.objectconstruction.framework.FrameworkSupportUtils;
import org.checkerframework.checker.objectconstruction.framework.LombokSupport;
import org.checkerframework.checker.objectconstruction.qual.CalledMethodsPredicate;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.springframework.expression.spel.SpelParseException;

public class ObjectConstructionVisitor
    extends BaseTypeVisitor<ObjectConstructionAnnotatedTypeFactory> {
  /** @param checker the type-checker associated with this visitor */
  public ObjectConstructionVisitor(final BaseTypeChecker checker) {
    super(checker);
  }

  /** Checks each @CalledMethodsPredicate annotation to make sure the predicate is well-formed. */
  @Override
  public Void visitAnnotation(final AnnotationTree node, final Void p) {
    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(node);
    if (AnnotationUtils.areSameByClass(anno, CalledMethodsPredicate.class)) {
      String predicate = AnnotationUtils.getElementValue(anno, "value", String.class, false);

      try {
        new CalledMethodsPredicateEvaluator(Collections.emptyList()).evaluate(predicate);
      } catch (SpelParseException e) {
        checker.report(Result.failure("predicate.invalid", e.getMessage()), node);
        return null;
      }
    }
    return super.visitAnnotation(node, p);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    ExecutableElement element = TreeUtils.elementFromUse(node);
    TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
    Element nextEnclosingElement = enclosingElement.getEnclosingElement();

    // autovalue builder
    if (FrameworkSupportUtils.hasAnnotation(enclosingElement, AutoValue.Builder.class)) {
      if (AutoValueSupport.isBuilderBuildMethod(element, nextEnclosingElement)) {
        ((ObjectConstructionChecker) checker).numBuildCalls++;
      }
    }

    // lombok builder
    if ((FrameworkSupportUtils.hasAnnotation(enclosingElement, "lombok.Generated")
            || FrameworkSupportUtils.hasAnnotation(element, "lombok.Generated"))
        && enclosingElement.getSimpleName().toString().endsWith("Builder")) {
      if (LombokSupport.isBuilderBuildMethod(element, nextEnclosingElement)) {
        ((ObjectConstructionChecker) checker).numBuildCalls++;
      }
    }

    return super.visitMethodInvocation(node, p);
  }
}
