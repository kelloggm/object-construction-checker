package org.checkerframework.checker.builder;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import org.checkerframework.checker.builder.lombok.LombokBuilderChecker;
import org.checkerframework.checker.builder.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The annotated type factory for the typesafe builder checker. Primarily responsible for the
 * subtyping rules between @CalledMethod annotations.
 */
public class TypesafeBuilderAnnotatedTypeFactory extends BaseAnnotatedTypeFactory
    implements CalledMethodsAnnotatedTypeFactory {

  /**
   * Canonical copies of the top and bottom annotations. Package private to permit access from the
   * Transfer class.
   */
  final AnnotationMirror TOP, BOTTOM;

  /** Default constructor matching super. Should be called automatically. */
  public TypesafeBuilderAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, CalledMethodsTop.class);
    BOTTOM = AnnotationBuilder.fromClass(elements, CalledMethodsBottom.class);
    System.out.println("ATF was initialized");
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            CalledMethodsTop.class,
            CalledMethods.class,
            CalledMethodsBottom.class,
            CalledMethodsPredicate.class));
  }

  private BaseAnnotatedTypeFactory getLombokCheckerAnnotatedTypeFactory() {
    return getTypeFactoryOfSubchecker(LombokBuilderChecker.class);
  }

  /* @Override
  public void addComputedTypeAnnotations(Element element, AnnotatedTypeMirror type) {
      super.addComputedTypeAnnotations(element, type);
      if (element != null) {
          AnnotatedTypeMirror lombokType = getLombokCheckerAnnotatedTypeFactory().getAnnotatedType(element);
          if (!lombokType.hasAnnotation(TOP) && !lombokType.hasAnnotation(BOTTOM)) {
              type.addAnnotation(lombokType.getAnnotationInHierarchy(TOP));
          }
      }
  }*/

  @Override
  public void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
    super.addComputedTypeAnnotations(tree, type, iUseFlow);
    if (iUseFlow && tree != null && TreeUtils.isExpressionTree(tree)) {
      AnnotatedTypeMirror lombokType =
          getLombokCheckerAnnotatedTypeFactory().getAnnotatedType(tree);
      AnnotationMirror lombokAnm = lombokType.getAnnotationInHierarchy(TOP);
      if (lombokAnm != null
          && !AnnotationUtils.areSame(lombokAnm, TOP)
          && !AnnotationUtils.areSame(lombokAnm, BOTTOM)) {
        type.addAnnotation(lombokAnm);
      }
    }
  }

  /** Creates a @CalledMethods annotation whose values are the given strings. */
  public AnnotationMirror createCalledMethods(final String... val) {
    return CalledMethodsUtil.createCalledMethodsImpl(TOP, processingEnv, val);
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new TypesafeBuilderTreeAnnotator(this));
  }

  @Override
  public QualifierHierarchy createQualifierHierarchy(
      final MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
    return new TypesafeBuilderQualifierHierarchy(factory, TOP, BOTTOM, this);
  }

  /**
   * This tree annotator is needed to create types for fluent builders that have @ReturnsReceiver
   * annotations.
   */
  private class TypesafeBuilderTreeAnnotator extends TreeAnnotator {
    public TypesafeBuilderTreeAnnotator(final AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitMethodInvocation(
        final MethodInvocationTree tree, final AnnotatedTypeMirror type) {

      // Check to see if the @ReturnsReceiver annotation is present
      Element element = TreeUtils.elementFromUse(tree);
      AnnotationMirror returnsReceiver = getDeclAnnotation(element, ReturnsReceiver.class);

      if (returnsReceiver != null) {

        // Fetch the current type of the receiver, or top if none exists
        ExpressionTree receiverTree = TreeUtils.getReceiverTree(tree.getMethodSelect());
        AnnotatedTypeMirror receiverType = getAnnotatedType(receiverTree);
        AnnotationMirror receiverAnno = receiverType.getAnnotationInHierarchy(TOP);
        if (receiverAnno == null) {
          receiverAnno = TOP;
        }

        // Construct a new @CM annotation with just the method name
        String methodName = TreeUtils.methodName(tree).toString();
        AnnotationMirror cmAnno = createCalledMethods(methodName);

        // Replace the return type of the method with the GLB (= union) of the two types above
        AnnotationMirror newAnno = getQualifierHierarchy().greatestLowerBound(cmAnno, receiverAnno);
        type.replaceAnnotation(newAnno);
      }

      return super.visitMethodInvocation(tree, type);
    }
  }
}
