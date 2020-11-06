package org.checkerframework.checker.mustcall;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/**
 * The visitor for the MustCall checker. This visitor is similar to BaseTypeVisitor, but overrides
 * methods that don't work well with the MustCall type hierarchy because it doesn't use the top type
 * as the default type.
 */
public class MustCallVisitor extends BaseTypeVisitor<MustCallAnnotatedTypeFactory> {

  /**
   * Creates a new MustCallVisitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public MustCallVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  protected boolean skipReceiverSubtypeCheck(
      MethodInvocationTree node,
      AnnotatedTypeMirror methodDefinitionReceiver,
      AnnotatedTypeMirror methodCallReceiver) {
    // TODO: Check explicit receiver parameters annotated with @Owning. ExecutableElement
    //       doesn't have any way to get an element associated with the receiver, so I can't
    //       figure out a way to get a declaration annotation for the receiver. It might not
    //       be possible? The below is the closest that I got, but the receiver doesn't show up
    //       in the list of the parameters, even when it's explicit. Is this a bug in javac?
    //
    //    ExecutableElement elt = TreeUtils.elementFromUse(node);
    //    System.out.println(elt);
    //    List<? extends VariableElement> params = elt.getParameters();
    //    if (!params.isEmpty()) {
    //      VariableElement first = params.get(0);
    //      if (first.getSimpleName().contentEquals("this")) {
    //        return atypeFactory.getDeclAnnotation(first, Owning.class) == null;
    //      }
    //    }
    return true;
  }

  /**
   * Does not issue any warnings.
   *
   * <p>This method typically issues a warning if the result type of the constructor is not top,
   * because in top-default type systems that indicates a potential problem. The must call checker
   * does not need this warning, because it expects the type of all constructors to be {@code
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
   * false positives. This is unsound; see the discussion on
   * https://github.com/typetools/checker-framework/issues/3839. TODO: change checking of throws
   * clauses to require that the thrown exception is @MustCall({}). This would probably eliminate
   * most of the same false positives, without adding undue false positives.
   *
   * @return a set containing only the @MustCall({}) annotation
   */
  @Override
  protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
    return Collections.singleton(atypeFactory.BOTTOM);
  }

  /**
   * Does not issue any warnings.
   *
   * <p>Annotation arguments are treated as return locations for the purposes of defaulting, rather
   * than parameter locations. This causes them to default incorrectly when the annotation is
   * defined in bytecode. See https://github.com/typetools/checker-framework/issues/3178 for an
   * explanation of why this is necessary to avoid false positives.
   *
   * <p>Skipping this check in the MustCall checker is sound, because the MustCall checker is not
   * concerned with annotation arguments (which must be literals, and therefore won't have (or be
   * able to fulfill) must-call obligations).
   */
  @Override
  public Void visitAnnotation(AnnotationTree node, Void p) {
    return null;
  }
}
