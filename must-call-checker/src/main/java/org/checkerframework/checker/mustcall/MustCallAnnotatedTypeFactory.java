package org.checkerframework.checker.mustcall;

import static org.checkerframework.common.value.ValueCheckerUtils.getValueOfAnnotationWithStringArgument;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
import org.checkerframework.checker.objectconstruction.qual.NotOwning;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The annotated type factory for the must call checker. Primarily responsible for the subtyping
 * rules between @MustCall annotations.
 */
public class MustCallAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The top annotation. */
  final AnnotationMirror TOP;

  /** The bottom annotation, which is the default in unannotated code. */
  final AnnotationMirror BOTTOM;

  /** The polymorphic qualifier */
  final AnnotationMirror POLY;

  /**
   * Default constructor matching super. Should be called automatically.
   *
   * @param checker the checker associated with this type factory
   */
  public MustCallAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, MustCallUnknown.class);
    BOTTOM = createMustCall();
    POLY = AnnotationBuilder.fromClass(elements, PolyMustCall.class);
    addAliasedAnnotation(InheritableMustCall.class, MustCall.class, true);
    this.postInit();
  }

  /** Treat non-owning method parameters as @MustCallUnknown. */
  @Override
  public void methodFromUsePreSubstitution(ExpressionTree tree, AnnotatedExecutableType type) {
    ExecutableElement declaration;
    if (tree instanceof MethodInvocationTree) {
      declaration = TreeUtils.elementFromUse((MethodInvocationTree) tree);
    } else if (tree instanceof MemberReferenceTree) {
      declaration = (ExecutableElement) TreeUtils.elementFromTree(tree);
    } else {
      throw new BugInCF("unexpected type of method tree: " + tree.getKind());
    }
    for (int i = 0; i < type.getParameterTypes().size(); i++) {
      Element paramDecl = declaration.getParameters().get(i);
      if (getDeclAnnotation(paramDecl, NotOwning.class) != null) {
        AnnotatedTypeMirror paramType = type.getParameterTypes().get(i);
        paramType.replaceAnnotation(TOP);
        // Descend into a varargs array
        if (declaration.isVarArgs() && i == declaration.getParameters().size() - 1) {
          AnnotatedTypeMirror componentType = ((AnnotatedArrayType) paramType).getComponentType();
          componentType.replaceAnnotation(TOP);
        }
      }
    }
    super.methodFromUsePreSubstitution(tree, type);
  }

  @Override
  public AnnotatedTypeMirror fromElement(Element elt) {
    AnnotatedTypeMirror type = super.fromElement(elt);
    // Support @InheritableMustCall meaning @MustCall on all class declaration elements.
    if (ElementUtils.isClassElement(elt)) {
      AnnotationMirror inheritableMustCall = getDeclAnnotation(elt, InheritableMustCall.class);
      if (inheritableMustCall != null) {
        List<String> mustCallVal =
            ValueCheckerUtils.getValueOfAnnotationWithStringArgument(inheritableMustCall);
        AnnotationMirror inheritedMCAnno = createMustCall(mustCallVal.toArray(new String[0]));
        // Ensure that there isn't an inconsistent, user-written @MustCall annotation and
        // issue an error if there is. Otherwise, replace the implicit @MustCall({}) with
        // the inherited must-call annotation.
        AnnotationMirror writtenMCAnno = type.getAnnotationInHierarchy(this.TOP);
        if (writtenMCAnno != null
            && !this.getQualifierHierarchy().isSubtype(inheritedMCAnno, writtenMCAnno)) {
          checker.reportError(
              elt,
              "inconsistent.mustcall.subtype",
              elt.getSimpleName(),
              writtenMCAnno,
              inheritableMustCall);
        } else {
          type.replaceAnnotation(inheritedMCAnno);
        }
      }
    }
    return type;
  }

  /**
   * Creates a {@link MustCall} annotation whose values are the given strings.
   *
   * @param val the methods that should be called
   * @return an annotation indicating that the given methods should be called
   */
  public AnnotationMirror createMustCall(final String... val) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MustCall.class);
    Arrays.sort(val);
    builder.setValue("value", val);
    return builder.build();
  }

  @Override
  public QualifierHierarchy createQualifierHierarchy() {
    return new MustCallQualifierHierarchy(this.getSupportedTypeQualifiers(), this.elements);
  }

  /**
   * The qualifier hierarchy is responsible for lub, glb, and subtyping between qualifiers without
   * declaratively-defined subtyping relationships, like our @MustCall annotation.
   */
  private class MustCallQualifierHierarchy extends ElementQualifierHierarchy {

    protected MustCallQualifierHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
      super(qualifierClasses, elements);
    }

    @Override
    public AnnotationMirror getTopAnnotation(final AnnotationMirror start) {
      return TOP;
    }

    /**
     * GLB in this type system is set intersection of the arguments of the two annotations, unless
     * one of the annotations is top (if so, the result is the other annotation).
     */
    @Override
    public AnnotationMirror greatestLowerBound(
        final AnnotationMirror a1, final AnnotationMirror a2) {

      // Shortcut for the common case, because bottom is the default and the intersection of
      // any set with the empty set (i.e. bottom) is also the empty set.
      if (AnnotationUtils.areSame(a1, BOTTOM) || AnnotationUtils.areSame(a2, BOTTOM)) {
        return BOTTOM;
      }

      if (isPolymorphicQualifier(a1) && isPolymorphicQualifier(a2)) {
        return a1;
      } else if (isPolymorphicQualifier(a1) || isPolymorphicQualifier(a2)) {
        return BOTTOM;
      }

      if (AnnotationUtils.areSame(a1, TOP)) {
        return a2;
      }

      if (AnnotationUtils.areSame(a2, TOP)) {
        return a1;
      }

      Set<String> a1Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a1));
      Set<String> a2Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a2));
      a1Val.retainAll(a2Val);
      return createMustCall(a1Val.toArray(new String[0]));
    }

    /**
     * LUB in this type system is set union of the arguments of the two annotations, unless one of
     * them is top, in which case the result is top.
     */
    @Override
    public AnnotationMirror leastUpperBound(final AnnotationMirror a1, final AnnotationMirror a2) {
      if (AnnotationUtils.areSame(a1, TOP) || AnnotationUtils.areSame(a2, TOP)) {
        return TOP;
      }

      if (isPolymorphicQualifier(a1) && isPolymorphicQualifier(a2)) {
        return a1;
      } else if (isPolymorphicQualifier(a1) || isPolymorphicQualifier(a2)) {
        return TOP;
      }

      Set<String> a1Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a1));
      Set<String> a2Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a2));
      a1Val.addAll(a2Val);
      return createMustCall(a1Val.toArray(new String[0]));
    }

    /** isSubtype in this type system is superset */
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

      if (isPolymorphicQualifier(subAnno)) {
        return isPolymorphicQualifier(superAnno);
      } else if (isPolymorphicQualifier(superAnno)) {
        // Polymorphic annotations are only a supertype of other polymorphic annotations and
        // the bottom type, both of which have already been checked above.
        return false;
      }

      Set<String> subVal = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(subAnno));
      Set<String> superVal = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(superAnno));

      return superVal.containsAll(subVal);
    }
  }
}
