package org.checkerframework.checker.builder;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
import org.checkerframework.javacutil.TypesUtils;

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

  /**
   * The list of annotations that Lombok treats as non-null. The list is copied from
   * lombok.core.handlers.HandlerUtil. The list cannot be used from that class directly because
   * Lombok does not actually provide class files for its own implementation, to prevent itself from
   * being accidentally added to clients' compile classpaths. This design decision means that it is
   * impossible to depend directly on Lombok internals.
   */
  public static final List<String> NONNULL_ANNOTATIONS;

  static {
    NONNULL_ANNOTATIONS =
        Collections.unmodifiableList(
            Arrays.asList(
                "android.annotation.NonNull",
                "android.support.annotation.NonNull",
                "com.sun.istack.internal.NotNull",
                "edu.umd.cs.findbugs.annotations.NonNull",
                "javax.annotation.Nonnull",
                // "javax.validation.constraints.NotNull", // The field might contain a null value
                // until it is persisted.
                "lombok.NonNull",
                "org.checkerframework.checker.nullness.qual.NonNull",
                "org.eclipse.jdt.annotation.NonNull",
                "org.eclipse.jgit.annotations.NonNull",
                "org.jetbrains.annotations.NotNull",
                "org.jmlspecs.annotation.NonNull",
                "org.netbeans.api.annotations.common.NonNull",
                "org.springframework.lang.NonNull"));
  }

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

  private enum BuilderKind {
    LOMBOK,
    AUTO_VALUE,
    NONE
  }

  /**
   * adds @CalledMethod annotations for build() methods of AutoValue and Lombok Builders to ensure
   * required properties have been set
   */
  private class TypesafeBuilderTypeAnnotator extends TypeAnnotator {

    public TypesafeBuilderTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {
      ExecutableElement element = t.getElement();

      String methodName = element.getSimpleName().toString();

      Element enclosingElement = element.getEnclosingElement();

      if (hasAnnotation(enclosingElement, AutoValue.class) && "toBuilder".equals(methodName)) {
        handleAutoValueToBuilder(t, enclosingElement);
        return super.visitExecutable(t, p);
      }

      Element nextEnclosingElement = enclosingElement.getEnclosingElement();
      if (!nextEnclosingElement.getKind().isClass()) {
        return super.visitExecutable(t, p);
      }

      BuilderKind builderKind = BuilderKind.NONE;

      if (hasAnnotation(enclosingElement, AutoValue.Builder.class)) {
        builderKind = BuilderKind.AUTO_VALUE;
        assert hasAnnotation(nextEnclosingElement, AutoValue.class)
            : "class " + nextEnclosingElement.getSimpleName() + " is missing @AutoValue annotation";

      } else if (hasAnnotation(enclosingElement, lombok.Generated.class)
          && enclosingElement.getSimpleName().toString().endsWith("Builder")) {
        builderKind = BuilderKind.LOMBOK;
      }

      if (builderKind != BuilderKind.NONE) {

        if ("build".equals(methodName)) {
          // determine the required properties and add a corresponding @CalledMethods annotation
          List<String> requiredProperties =
              getRequiredProperties(nextEnclosingElement, builderKind);
          AnnotationMirror newCalledMethodsAnno =
              createCalledMethodsForProperties(
                  requiredProperties, getAllMethodNames(enclosingElement), builderKind);
          t.getReceiverType().addAnnotation(newCalledMethodsAnno);
        }
      }

      return super.visitExecutable(t, p);
    }

    private Set<String> getAllMethodNames(Element enclosingElement) {
      return enclosingElement.getEnclosedElements().stream()
          .filter(e -> e.getKind().equals(ElementKind.METHOD))
          .map(e -> e.getSimpleName().toString())
          .collect(Collectors.toSet());
    }

    /**
     * For an AutoValue toBuilder routine, we know that the returned Builder effectively has had all
     * the required setters invoked. Add a CalledMethods annotation capturing this fact.
     *
     * @param t type of toBuilder method
     * @param autoValueClassElement enclosing AutoValue class
     */
    private void handleAutoValueToBuilder(
        AnnotatedTypeMirror.AnnotatedExecutableType t, Element autoValueClassElement) {
      AnnotatedTypeMirror returnType = t.getReturnType();
      Element builderElement = TypesUtils.getTypeElement(returnType.getUnderlyingType());
      List<String> requiredProperties =
          getRequiredProperties(autoValueClassElement, BuilderKind.AUTO_VALUE);
      AnnotationMirror calledMethodsAnno =
          createCalledMethodsForAutoValueProperties(
              requiredProperties, getAllMethodNames(builderElement));
      returnType.addAnnotation(calledMethodsAnno);
    }

    /**
     * computes the required properties of a builder class
     *
     * @param builderElement the class whose builder is to be checked
     * @param builderKind the framework by which the builder will be generated
     * @return a list of required property names
     */
    private List<String> getRequiredProperties(
        final Element builderElement, final BuilderKind builderKind) {
      switch (builderKind) {
        case AUTO_VALUE:
          return getAutoValueRequiredProperties(builderElement);
        case LOMBOK:
          return getLombokRequiredProperties(builderElement);
        default:
          return Collections.emptyList();
      }
    }

    /**
     * computes the required properties of a @lombok.Builder class, i.e., the names of the fields
     * with @lombok.NonNull annotations
     *
     * @param lombokClassElement the class with the @lombok.Builder annotation
     * @return a list of required property names
     */
    private List<String> getLombokRequiredProperties(final Element lombokClassElement) {
      List<String> requiredPropertyNames = new ArrayList<>();
      List<String> defaultedPropertyNames = new ArrayList<>();
      for (Element member : lombokClassElement.getEnclosedElements()) {
        if (member.getKind().equals(ElementKind.FIELD)) {
          // VariableTree fieldTree = (VariableTree) member;
          for (AnnotationMirror anm : elements.getAllAnnotationMirrors(member)) {
            if (NONNULL_ANNOTATIONS.contains(AnnotationUtils.annotationName(anm))) {
              requiredPropertyNames.add(member.getSimpleName().toString());
            }
          }
        } else if (member.getKind().equals(ElementKind.METHOD)) {
          String methodName = member.getSimpleName().toString();
          if (methodName.startsWith("$default$")) {
            String propName = methodName.substring(9); // $default$ has 9 characters
            defaultedPropertyNames.add(propName);
          }
        }
      }
      requiredPropertyNames.removeAll(defaultedPropertyNames);
      return requiredPropertyNames;
    }

    /**
     * computes the required properties of an @AutoValue class, i.e., those methods returning some
     * non-void, non-@Nullable type
     *
     * @param autoValueClassElement the @AutoValue class
     * @return a list of required property names
     */
    private List<String> getAutoValueRequiredProperties(final Element autoValueClassElement) {
      List<String> requiredPropertyNames = new ArrayList<>();
      for (Element member : autoValueClassElement.getEnclosedElements()) {
        if (member.getKind().equals(ElementKind.METHOD)) {
          // should be an abstract instance method
          Set<Modifier> modifiers = member.getModifiers();
          if (!modifiers.contains(Modifier.STATIC) && modifiers.contains(Modifier.ABSTRACT)) {
            String name = member.getSimpleName().toString();
            if (!IGNORED_METHOD_NAMES.contains(name)
                && !((ExecutableElement) member).getReturnType().toString().equals("void")) {
              // shouldn't have a nullable return
              boolean hasNullable =
                  Stream.concat(
                          elements.getAllAnnotationMirrors(member).stream(),
                          ((ExecutableElement) member)
                              .getReturnType().getAnnotationMirrors().stream())
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
     * @param allBuilderMethodNames names of all methods in the builder class
     * @param builderKind the kind of builder
     * @return the @CalledMethods annotation
     */
    public AnnotationMirror createCalledMethodsForProperties(
        final List<String> propertyNames,
        Set<String> allBuilderMethodNames,
        final BuilderKind builderKind) {
      switch (builderKind) {
        case AUTO_VALUE:
          return createCalledMethodsForAutoValueProperties(propertyNames, allBuilderMethodNames);
        case LOMBOK:
          return createCalledMethodsForLombokProperties(propertyNames);
        default:
          return TOP;
      }
    }

    private AnnotationMirror createCalledMethodsForLombokProperties(List<String> propertyNames) {
      return createCalledMethods(propertyNames.toArray(new String[0]));
    }

    private AnnotationMirror createCalledMethodsForAutoValueProperties(
        final List<String> propertyNames, Set<String> allBuilderMethodNames) {
      String[] calledMethodNames =
          propertyNames.stream()
              .map(prop -> autoValuePropToBuilderSetterName(prop, allBuilderMethodNames))
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

  private boolean hasAnnotation(Element element, Class<? extends Annotation> annotClass) {
    return elements.getAllAnnotationMirrors(element).stream()
        .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
  }

  private static String autoValuePropToBuilderSetterName(
      String prop, Set<String> allBuilderMethodNames) {
    // we have two cases, depending on whether AutoValue strips JavaBean-style prefixes 'get' and
    // 'is'
    Set<String> possiblePropNames = new LinkedHashSet<>();
    possiblePropNames.add(prop);
    if (prop.startsWith("get") && prop.length() > 3 && Character.isUpperCase(prop.charAt(3))) {
      possiblePropNames.add(Introspector.decapitalize(prop.substring(3)));
    } else if (prop.startsWith("is")
        && prop.length() > 2
        && Character.isUpperCase(prop.charAt(2))) {
      possiblePropNames.add(Introspector.decapitalize(prop.substring(2)));
    }

    for (String propName : possiblePropNames) {
      // in each case, the setter may be the property name itself, or prefixed by 'set'
      ImmutableSet<String> setterNamesToTry =
          ImmutableSet.of(propName, "set" + capitalize(propName));
      for (String setterName : setterNamesToTry) {
        if (allBuilderMethodNames.contains(setterName)) {
          return setterName;
        }
      }
    }

    // nothing worked
    throw new RuntimeException("could not find Builder setter name for property " + prop);
  }

  private static String capitalize(String prop) {
    return prop.substring(0, 1).toUpperCase() + prop.substring(1);
  }

  /**
   * Ignore java.lang.Object overrides, constructors, and toBuilder method in AutoValue classes.
   *
   * <p>Strictly speaking we should probably be checking return types, etc. here to handle strange
   * overloads and other corner cases. They seem unlikely enough that we are skipping for now.
   */
  private static final ImmutableSet<String> IGNORED_METHOD_NAMES =
      ImmutableSet.of("equals", "hashCode", "toString", "<init>", "toBuilder");
}
