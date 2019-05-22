package org.checkerframework.checker.builder.autovalue;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import org.checkerframework.checker.builder.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.builder.CalledMethodsUtil;
import org.checkerframework.checker.builder.TypesafeBuilderQualifierHierarchy;
import org.checkerframework.checker.builder.qual.CalledMethods;
import org.checkerframework.checker.builder.qual.CalledMethodsBottom;
import org.checkerframework.checker.builder.qual.CalledMethodsPredicate;
import org.checkerframework.checker.builder.qual.CalledMethodsTop;
import org.checkerframework.com.google.common.collect.ImmutableSet;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/** Responsible for placing appropriate annotations on Lombok builders. */
public class AutoValueBuilderAnnotatedTypeFactory extends BaseAnnotatedTypeFactory
    implements CalledMethodsAnnotatedTypeFactory {

  private final AnnotationMirror TOP, BOTTOM;

  public AutoValueBuilderAnnotatedTypeFactory(BaseTypeChecker checker) {
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

  /** Creates a @CalledMethods annotation whose values are the given strings. */
  public AnnotationMirror createCalledMethods(final String... val) {
    return CalledMethodsUtil.createCalledMethodsImpl(TOP, processingEnv, val);
  }

  /**
   * Wrapper to accept a List of Strings instead of an array if that's convenient at the call site.
   */
  public AnnotationMirror createCalledMethods(final List<String> requiredProperties) {
    List<String> calledMethodNames =
        requiredProperties.stream()
            .map((prop) -> "set" + prop.substring(0, 1).toUpperCase() + prop.substring(1))
            .collect(Collectors.toList());
    return createCalledMethods(calledMethodNames.toArray(new String[0]));
  }

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(
        super.createTypeAnnotator(), new AutoValueBuilderTypeAnnotator(this));
  }

  private class AutoValueBuilderTypeAnnotator extends TypeAnnotator {

    public AutoValueBuilderTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {
      MethodTree methodTree = (MethodTree) declarationFromElement(t.getElement());
      if (methodTree == null) {
        return super.visitExecutable(t, p);
      }
      ClassTree enclosingClass = TreeUtils.enclosingClass(getPath(methodTree));

      if (enclosingClass == null) {
        return super.visitExecutable(t, p);
      }

      boolean inAutoValueBuilder = hasAnnotation(enclosingClass, AutoValue.Builder.class);

      if (inAutoValueBuilder) {
        // get the name of the method
        String methodName = methodTree.getName().toString();

        ClassTree autoValueClass =
            TreeUtils.enclosingClass(getPath(enclosingClass).getParentPath());

        assert hasAnnotation(autoValueClass, AutoValue.class)
            : "class " + autoValueClass.getSimpleName() + " is missing @AutoValue annotation";

        if ("build".equals(methodName)) {
          // if its a finalizer, add the @CalledMethods annotation with the field names
          // to the receiver
          List<String> requiredProperties = getRequiredProperties(autoValueClass);
          AnnotationMirror newCalledMethodsAnno = createCalledMethods(requiredProperties);
          t.getReceiverType().addAnnotation(newCalledMethodsAnno);
        }
      }

      return super.visitExecutable(t, p);
    }

    private List<String> getRequiredProperties(ClassTree autoValueClass) {
      List<String> requiredPropertyNames = new ArrayList<>();
      for (Tree member : autoValueClass.getMembers()) {
        if (member.getKind() == Tree.Kind.METHOD) {
          MethodTree methodTree = (MethodTree) member;
          // should be an instance method
          if (!methodTree.getModifiers().getFlags().contains(Modifier.STATIC)) {
            String name = methodTree.getName().toString();
            if (!IGNORED_METHOD_NAMES.contains(name)
                && !methodTree.getReturnType().toString().equals("void")) {
              // shouldn't have a nullable return
              List<? extends AnnotationTree> annotations =
                  methodTree.getModifiers().getAnnotations();
              boolean hasNullable =
                  annotations.stream()
                      .map(TreeUtils::annotationFromAnnotationTree)
                      .anyMatch(anm -> AnnotationUtils.annotationName(anm).endsWith(".Nullable"));
              if (!hasNullable) {
                requiredPropertyNames.add(name);
              }
            }
          }
        }
      }
      return requiredPropertyNames;
    }
  }

  private static boolean hasAnnotation(
      ClassTree enclosingClass, Class<? extends Annotation> annotClass) {
    return enclosingClass.getModifiers().getAnnotations().stream()
        .map(TreeUtils::annotationFromAnnotationTree)
        .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
  }

  @Override
  public MultiGraphQualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
    return new TypesafeBuilderQualifierHierarchy(factory, TOP, BOTTOM, this);
  }

  private static final ImmutableSet<String> IGNORED_METHOD_NAMES =
      ImmutableSet.of("equals", "hashCode", "toString", "<init>");
}
