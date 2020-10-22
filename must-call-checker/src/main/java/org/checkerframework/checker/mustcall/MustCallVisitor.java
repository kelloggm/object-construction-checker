package org.checkerframework.checker.mustcall;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The visitor for the MustCall checker. This visitor is similar to BaseTypeVisitor, but overrides
 * methods that don't work well with the MustCall type hierarchy because it doesn't use the top type
 * as the default type.
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

  /**
   * Only check type arguments that are bottom, to avoid false positives.
   *
   * <p>Without this code, the checker issues false positives at any point where a type with a
   * non-default @MustCall annotation is used as a type argument. For example, if the user writes
   * {@code List<Socket>} (and Socket is must-call close), then without this code a
   * type.argument.type.incompatible error is issued. These errors are spurious, because the List of
   * Sockets does in fact have a responsibility to close each of those sockets.
   */
  protected void checkTypeArguments(
      Tree toptree,
      List<? extends AnnotatedTypeParameterBounds> paramBounds,
      List<? extends AnnotatedTypeMirror> typeargs,
      List<? extends Tree> typeargTrees,
      Name typeOrMethodName,
      List<?> paramNames) {
    List<Integer> indicesToRemove = new ArrayList<>();
    for (int i = 0; i < typeargs.size(); i++) {
      AnnotatedTypeMirror atm = typeargs.get(i);
      if (containsNonDefaultAnnotation(atm)) {
        indicesToRemove.add(i);
      }
    }
    // The lists are unmodifiable, so new lists have to be constructed
    // rather than removing from the old ones.
    List<AnnotatedTypeParameterBounds> paramBoundsWithoutNonDefault = new ArrayList<>();
    List<AnnotatedTypeMirror> typeargsWithoutNonDefault = new ArrayList<>();
    for (int i = 0; i < typeargs.size(); i++) {
      if (!indicesToRemove.contains(i)) {
        paramBoundsWithoutNonDefault.add(paramBounds.get(i));
        typeargsWithoutNonDefault.add(typeargs.get(i));
      }
    }
    super.checkTypeArguments(
        toptree,
        paramBoundsWithoutNonDefault,
        typeargsWithoutNonDefault,
        typeargTrees,
        typeOrMethodName,
        paramNames);
  }

  /**
   * Does a deep scan of the annotated type for any non-default (that is, non-bottom) annotation.
   * Returns true iff the type or one of its component types contains a non-default type.
   */
  private boolean containsNonDefaultAnnotation(final AnnotatedTypeMirror type) {
    switch (type.getKind()) {
      case DECLARED:
        AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
        for (AnnotatedTypeMirror typearg : declaredType.getTypeArguments()) {
          if (containsNonDefaultAnnotation(typearg)) {
            return true;
          }
        }
        return !AnnotationUtils.areSame(
            type.getAnnotationInHierarchy(atypeFactory.TOP), atypeFactory.BOTTOM);
      case ARRAY:
        AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
        return containsNonDefaultAnnotation(arrayType.getComponentType())
            || !AnnotationUtils.areSame(
                type.getAnnotationInHierarchy(atypeFactory.TOP), atypeFactory.BOTTOM);
      default:
        return false;
    }
  }
}
