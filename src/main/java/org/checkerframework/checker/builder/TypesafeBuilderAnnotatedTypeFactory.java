package org.checkerframework.checker.builder;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.*;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import org.checkerframework.checker.builder.qual.CalledMethods;
import org.checkerframework.checker.builder.qual.CalledMethodsBottom;
import org.checkerframework.checker.builder.qual.CalledMethodsPredicate;
import org.checkerframework.checker.builder.qual.CalledMethodsTop;
import org.checkerframework.checker.returnsrcvr.ReturnsRcvrAnnotatedTypeFactory;
import org.checkerframework.checker.returnsrcvr.ReturnsRcvrChecker;
import org.checkerframework.checker.returnsrcvr.qual.This;
import org.checkerframework.com.google.common.collect.ImmutableSet;
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
public class TypesafeBuilderAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

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

  /** Creates a @CalledMethods annotation whose values are the given strings. */
  public AnnotationMirror createCalledMethods(final String... val) {
    if (val.length == 0) {
      return TOP;
    }
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, CalledMethods.class);
    Arrays.sort(val);
    builder.setValue("value", val);
    return builder.build();
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
    return new TypesafeBuilderQualifierHierarchy(factory);
  }

  private ReturnsRcvrAnnotatedTypeFactory getReturnsRcvrAnnotatedTypeFactory() {
    return getTypeFactoryOfSubchecker(ReturnsRcvrChecker.class);
  }

  /**
   * This tree annotator is needed to create types for fluent builders that have @This annotations.
   */
  private class TypesafeBuilderTreeAnnotator extends TreeAnnotator {
    public TypesafeBuilderTreeAnnotator(final AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitMethodInvocation(
        final MethodInvocationTree tree, final AnnotatedTypeMirror type) {

      // Check to see if the ReturnsReceiver Checker has a @This annotation
      // on the return type of the method

      ReturnsRcvrAnnotatedTypeFactory rrATF = getReturnsRcvrAnnotatedTypeFactory();
      ExecutableElement methodEle = TreeUtils.elementFromUse(tree);
      AnnotatedTypeMirror methodATm = rrATF.getAnnotatedType(methodEle);
      AnnotatedTypeMirror rrType =
          ((AnnotatedTypeMirror.AnnotatedExecutableType) methodATm).getReturnType();

      if (rrType != null && rrType.hasAnnotation(This.class)) {

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

  /**
   * adds @CalledMethod annotations for build() methods of AutoValue Builders to ensure required
   * properties have been set
   */
  private class TypesafeBuilderTypeAnnotator extends TypeAnnotator {

    public TypesafeBuilderTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
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
          // determine the required properties and add a corresponding @CalledMethods annotation
          List<String> requiredProperties = getRequiredProperties(autoValueClass);
          AnnotationMirror newCalledMethodsAnno =
              createCalledMethodsForProperties(requiredProperties);
          t.getReceiverType().addAnnotation(newCalledMethodsAnno);
        }
      }

      return super.visitExecutable(t, p);
    }

    /**
     * computes the required properties of an @AutoValue class
     *
     * @param autoValueClass
     * @return
     */
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

    /**
     * creates a @CalledMethods annotation for the given property names, converting the names to the
     * corresponding setter method name in the Builder
     *
     * @param propertyNames the property names
     * @return the @CalledMethods annotation
     */
    public AnnotationMirror createCalledMethodsForProperties(final List<String> propertyNames) {
      String[] calledMethodNames =
          propertyNames.stream()
              .map((prop) -> "set" + prop.substring(0, 1).toUpperCase() + prop.substring(1))
              .toArray(String[]::new);
      return createCalledMethods(calledMethodNames);
    }
  }
  /**
   * The qualifier hierarchy is responsible for lub, glb, and subtyping between qualifiers without
   * declaratively defined subtyping relationships, like our @CalledMethods annotation.
   */
  private class TypesafeBuilderQualifierHierarchy extends MultiGraphQualifierHierarchy {
    public TypesafeBuilderQualifierHierarchy(
        final MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
      super(factory);
    }

    @Override
    public AnnotationMirror getTopAnnotation(final AnnotationMirror start) {
      return TOP;
    }

    /**
     * GLB in this type system is set union of the arguments of the two annotations, unless one of
     * them is bottom, in which case the result is also bottom.
     */
    @Override
    public AnnotationMirror greatestLowerBound(
        final AnnotationMirror a1, final AnnotationMirror a2) {
      if (AnnotationUtils.areSame(a1, BOTTOM) || AnnotationUtils.areSame(a2, BOTTOM)) {
        return BOTTOM;
      }

      if (!AnnotationUtils.hasElementValue(a1, "value")) {
        return a2;
      }

      if (!AnnotationUtils.hasElementValue(a2, "value")) {
        return a1;
      }

      Set<String> a1Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a1));
      Set<String> a2Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a2));
      a1Val.addAll(a2Val);
      return createCalledMethods(a1Val.toArray(new String[0]));
    }

    /**
     * LUB in this type system is set intersection of the arguments of the two annotations, unless
     * one of them is bottom, in which case the result is the other annotation.
     */
    @Override
    public AnnotationMirror leastUpperBound(final AnnotationMirror a1, final AnnotationMirror a2) {
      if (AnnotationUtils.areSame(a1, BOTTOM)) {
        return a2;
      } else if (AnnotationUtils.areSame(a2, BOTTOM)) {
        return a1;
      }

      if (!AnnotationUtils.hasElementValue(a1, "value")) {
        return a1;
      }

      if (!AnnotationUtils.hasElementValue(a2, "value")) {
        return a2;
      }

      Set<String> a1Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a1));
      Set<String> a2Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a2));
      a1Val.retainAll(a2Val);
      return createCalledMethods(a1Val.toArray(new String[0]));
    }

    /** isSubtype in this type system is subset */
    @Override
    public boolean isSubtype(final AnnotationMirror subAnno, final AnnotationMirror superAnno) {
      if (AnnotationUtils.areSame(subAnno, BOTTOM)) {
        return true;
      } else if (AnnotationUtils.areSame(superAnno, BOTTOM)) {
        return false;
      }

      if (AnnotationUtils.areSame(superAnno, TOP)) {
        return true;
      } else if (AnnotationUtils.areSame(subAnno, TOP)) {
        return false;
      }

      if (AnnotationUtils.areSameByClass(subAnno, CalledMethodsPredicate.class)) {
        return false;
      }

      Set<String> subVal = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(subAnno));

      if (AnnotationUtils.areSameByClass(superAnno, CalledMethodsPredicate.class)) {
        // superAnno is a CMP annotation, so we need to evaluate the predicate
        String predicate = AnnotationUtils.getElementValue(superAnno, "value", String.class, false);
        CalledMethodsPredicateEvaluator evaluator = new CalledMethodsPredicateEvaluator(subVal);
        return evaluator.evaluate(predicate);
      } else {
        // superAnno is a CM annotation, so compare the sets
        return subVal.containsAll(getValueOfAnnotationWithStringArgument(superAnno));
      }
    }
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

  private static boolean hasAnnotation(
      ClassTree enclosingClass, Class<? extends Annotation> annotClass) {
    return enclosingClass.getModifiers().getAnnotations().stream()
        .map(TreeUtils::annotationFromAnnotationTree)
        .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
  }

  /** ignore java.lang.Object overrides and constructors in AutoValue classes */
  private static final ImmutableSet<String> IGNORED_METHOD_NAMES =
      ImmutableSet.of("equals", "hashCode", "toString", "<init>");
}
