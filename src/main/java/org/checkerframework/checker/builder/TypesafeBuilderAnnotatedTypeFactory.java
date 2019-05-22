package org.checkerframework.checker.builder;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.*;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import org.checkerframework.checker.builder.autovalue.AutoValueBuilderChecker;
import org.checkerframework.checker.builder.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

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

  private BaseAnnotatedTypeFactory getAutoValueBuilderCheckerAnnotatedTypeFactory() {
    return getTypeFactoryOfSubchecker(AutoValueBuilderChecker.class);
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
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(
        super.createTypeAnnotator(), new TypesafeBuilderTypeAnnotator(this));
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

      if (hasReturnsReceiver(tree)) {

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

    private boolean hasReturnsReceiver(MethodInvocationTree tree) {
      // Check to see if the @ReturnsReceiver annotation is present
      Element element = TreeUtils.elementFromUse(tree);
      if (getDeclAnnotation(element, ReturnsReceiver.class) != null) {
        return true;
      }
      return isAutoValueBuilderSetter(element);
    }

    private boolean isAutoValueBuilderSetter(Element element) {
      MethodTree methodTree = (MethodTree) declarationFromElement(element);
      if (methodTree == null) {
        return false;
      }

      if (!methodTree.getModifiers().getFlags().contains(Modifier.ABSTRACT)) {
        return false;
      }
      ClassTree enclosingClass = TreeUtils.enclosingClass(getPath(methodTree));

      if (enclosingClass == null) {
        return false;
      }
      //      System.out.println(node.getName().toString() + " is visited method in class " +
      // enclosingClass.getSimpleName());

      boolean inAutoValueBuilder = hasAnnotation(enclosingClass, AutoValue.Builder.class);

      if (inAutoValueBuilder) {
        Element classElem = TreeUtils.elementFromTree(enclosingClass);
        Element returnTypeElem = TreeUtils.elementFromTree(methodTree.getReturnType());
        return classElem.equals(returnTypeElem);
      }
      return false;
    }
  }

  private static boolean hasAnnotation(
      ClassTree enclosingClass, Class<? extends Annotation> annotClass) {
    return enclosingClass.getModifiers().getAnnotations().stream()
        .map(TreeUtils::annotationFromAnnotationTree)
        .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
  }

  private class TypesafeBuilderTypeAnnotator extends TypeAnnotator {
    public TypesafeBuilderTypeAnnotator(AnnotatedTypeFactory aTypeFactory) {
      super(aTypeFactory);
    }

    @Override
    public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {
      MethodTree tree = (MethodTree) declarationFromElement(t.getElement());
      if (tree == null) {
        return super.visitExecutable(t, p);
      }
      AnnotatedTypeMirror avBuilderType =
          getAutoValueBuilderCheckerAnnotatedTypeFactory().getAnnotatedType(tree);
      AnnotatedTypeMirror.AnnotatedDeclaredType receiverType =
          ((AnnotatedTypeMirror.AnnotatedExecutableType) avBuilderType).getReceiverType();

      if (receiverType != null) {
        AnnotationMirror avBuilderAnm = receiverType.getAnnotationInHierarchy(TOP);
        if (avBuilderAnm != null
            && !AnnotationUtils.areSame(avBuilderAnm, TOP)
            && !AnnotationUtils.areSame(avBuilderAnm, BOTTOM)) {
          AnnotatedTypeMirror.AnnotatedDeclaredType origReceiverType = t.getReceiverType();
          AnnotationMirror receiverAnno = origReceiverType.getAnnotationInHierarchy(TOP);
          if (receiverAnno == null) {
            receiverAnno = TOP;
          }

          AnnotationMirror newAnno =
              getQualifierHierarchy().greatestLowerBound(avBuilderAnm, receiverAnno);
          origReceiverType.replaceAnnotation(newAnno);
        }
      }

      return super.visitExecutable(t, p);
    }
  }
}
