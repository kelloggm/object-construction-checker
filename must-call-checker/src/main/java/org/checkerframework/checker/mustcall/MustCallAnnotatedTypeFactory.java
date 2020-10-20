package org.checkerframework.checker.mustcall;

import static org.checkerframework.common.value.ValueCheckerUtils.getValueOfAnnotationWithStringArgument;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallTop;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The annotated type factory for the must call checker. Primarily responsible for the subtyping
 * rules between @MustCall annotations.
 */
public class MustCallAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The top annotation. */
  final AnnotationMirror TOP;

  /** The bottom annotation. */
  final AnnotationMirror BOTTOM;

  /**
   * Default constructor matching super. Should be called automatically.
   *
   * @param checker the checker associated with this type factory
   */
  public MustCallAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, MustCallTop.class);
    BOTTOM = createMustCall();
    this.postInit();
  }

  /**
   * Creates a @MustCall annotation whose values are the given strings.
   *
   * @param val the methods that have been called
   * @return an annotation indicating that the given methods have been called
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
   * declaratively defined subtyping relationships, like our @CalledMethods annotation.
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
     * GLB in this type system is set union of the arguments of the two annotations, unless one of
     * them is bottom, in which case the result is also bottom.
     */
    @Override
    public AnnotationMirror greatestLowerBound(
        final AnnotationMirror a1, final AnnotationMirror a2) {

      // shortcut for the common case
      if (AnnotationUtils.areSame(a1, BOTTOM) || AnnotationUtils.areSame(a2, BOTTOM)) {
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

      Set<String> subVal = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(subAnno));
      Set<String> superVal = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(superAnno));

      return superVal.containsAll(subVal);
    }
  }
}
