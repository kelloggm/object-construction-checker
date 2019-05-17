package org.checkerframework.checker.builder;

import static org.checkerframework.checker.builder.CalledMethodsUtil.getValueOfAnnotationWithStringArgument;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.builder.qual.CalledMethodsPredicate;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The qualifier hierarchy is responsible for lub, glb, and subtyping between qualifiers without
 * declaratively defined subtyping relationships, like our @CalledMethods annotation.
 */
public class TypesafeBuilderQualifierHierarchy extends MultiGraphQualifierHierarchy {
  private final AnnotationMirror top, bottom;
  private final CalledMethodsAnnotatedTypeFactory typeFactory;

  public TypesafeBuilderQualifierHierarchy(
      final MultiGraphQualifierHierarchy.MultiGraphFactory factory,
      final AnnotationMirror top,
      final AnnotationMirror bottom,
      final CalledMethodsAnnotatedTypeFactory typeFactory) {
    super(factory);
    this.top = top;
    this.bottom = bottom;
    this.typeFactory = typeFactory;
  }

  @Override
  public AnnotationMirror getTopAnnotation(final AnnotationMirror start) {
    return top;
  }

  /**
   * GLB in this type system is set union of the arguments of the two annotations, unless one of
   * them is bottom, in which case the result is also bottom.
   */
  @Override
  public AnnotationMirror greatestLowerBound(final AnnotationMirror a1, final AnnotationMirror a2) {
    if (AnnotationUtils.areSame(a1, bottom) || AnnotationUtils.areSame(a2, bottom)) {
      return bottom;
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
    return typeFactory.createCalledMethods(a1Val.toArray(new String[0]));
  }

  /**
   * LUB in this type system is set intersection of the arguments of the two annotations, unless one
   * of them is bottom, in which case the result is the other annotation.
   */
  @Override
  public AnnotationMirror leastUpperBound(final AnnotationMirror a1, final AnnotationMirror a2) {
    if (AnnotationUtils.areSame(a1, bottom)) {
      return a2;
    } else if (AnnotationUtils.areSame(a2, bottom)) {
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
    return typeFactory.createCalledMethods(a1Val.toArray(new String[0]));
  }

  /** isSubtype in this type system is subset */
  @Override
  public boolean isSubtype(final AnnotationMirror subAnno, final AnnotationMirror superAnno) {
    if (AnnotationUtils.areSame(subAnno, bottom)) {
      return true;
    } else if (AnnotationUtils.areSame(superAnno, bottom)) {
      return false;
    }

    if (AnnotationUtils.areSame(superAnno, top)) {
      return true;
    } else if (AnnotationUtils.areSame(subAnno, top)) {
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
