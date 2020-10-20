package org.checkerframework.checker.mustcall;

import com.sun.source.tree.AnnotationTree;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/**
 * The visitor for the MustCall checker. This visitor is similar to BaseTypeVisitor, but overrides
 * methods that don't work well with the MustCall type hierarchy because it doesn't use the top type as
 * the default type.
 */
public class MustCallVisitor extends BaseTypeVisitor<MustCallAnnotatedTypeFactory> {

  /** @param checker the type-checker associated with this visitor */
  public MustCallVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Typically issues a warning if the result type of the constructor is not top. This is not a
   * problem for the must call hierarchy, which expects the type of all constructors to be {@code
   * MustCall({})} (by default) or some other {@code MustCall} type, not the top type.
   *
   * @param constructorType AnnotatedExecutableType for the constructor
   * @param constructorElement element that declares the constructor
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    // Do nothing
  }

  /**
   * Change the default for exception parameter lower bounds to bottom (the default), to prevent
   * false positives. I think it might be a bug in the Checker Framework that these locations are
   * always defaulted to top - that doesn't make sense for checkers that use bottom as the default.
   *
   * @return a set containing only the @MustCall({}) annotation
   */
  @Override
  protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
    return Collections.singleton(atypeFactory.BOTTOM);
  }

  /**
   * The Checker Framework's default implementation of this method defers to {@code
   * #getExceptionParameterLowerBoundAnnotations}. That is a bug; this method should always return
   * the set containing top, regardless of what that method returns. This implementation does so.
   *
   * @return a set containing only the @MustCallTop annotation
   */
  @Override
  protected Set<? extends AnnotationMirror> getThrowUpperBoundAnnotations() {
    return Collections.singleton(atypeFactory.TOP);
  }

  /**
   * Annotation arguments are treated as return locations for the purposes of defaulting, rather
   * than parameter locations. This causes them to default incorrectly when the annotation is
   * defined in bytecode. See https://github.com/typetools/checker-framework/issues/3178 for an
   * explanation of why this is necessary to avoid false positives.
   *
   * <p>Skipping this check in the MustCall checker is safe, because the MustCall checker is not
   * concerned with annotation arguments (which must be literals, and therefore won't have (or be
   * able to fulfill) must-call obligations).
   */
  @Override
  public Void visitAnnotation(AnnotationTree node, Void p) {
    return null;
  }
}
