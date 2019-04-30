package org.checkerframework.checker.builder;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import org.checkerframework.checker.builder.qual.CalledMethods;
import org.checkerframework.checker.builder.qual.CalledMethodsBottom;
import org.checkerframework.checker.index.IndexUtil;
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

import javax.lang.model.element.AnnotationMirror;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The annotated type factory for the typesafe builder checker. Primarily responsible
 * for the subtyping rules between @CalledMethod annotations.
 */
public class TypesafeBuilderAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Canonical copies of the top and bottom annotations. Package private to permit access
     * from the Transfer class.
     */
    final AnnotationMirror TOP, BOTTOM;

    /**
     * Default constructor matching super. Should be called automatically.
     */
    public TypesafeBuilderAnnotatedTypeFactory(final BaseTypeChecker checker) {
        super(checker);
        TOP = createCalledMethods();
        BOTTOM = AnnotationBuilder.fromClass(elements, CalledMethodsBottom.class);
        this.postInit();
    }

    /** Creates a @CalledMethods annotation whose values are the given strings. */
    public AnnotationMirror createCalledMethods(final String... val) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, CalledMethods.class);
        Arrays.sort(val);
        builder.setValue("value", val);
        return builder.build();
    }

  /*  @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(), new TypesafeBuilderTreeAnnotator(this));
    }*/

    /**
     * This tree annotator is needed to create types for fluent builders:
     * new Builder().a().b().build() doesn't have a representation in FlowExpressions
     */
    private class TypesafeBuilderTreeAnnotator extends TreeAnnotator {
        public TypesafeBuilderTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {

            String receiverWithMethodName = tree.getMethodSelect().toString();

            if (!"super".equals(receiverWithMethodName)) {
                String methodName = receiverWithMethodName.contains(".") ?
                        receiverWithMethodName.substring(receiverWithMethodName.lastIndexOf('.') + 1)
                        : receiverWithMethodName;

                AnnotationMirror cmAnno = createCalledMethods(methodName);
                AnnotationMirror oldCMAnno = type.getAnnotationInHierarchy(TOP);
                AnnotationMirror newCMAnno =
                        oldCMAnno == null ?
                        cmAnno
                        : getQualifierHierarchy().greatestLowerBound(cmAnno, oldCMAnno);

                type.replaceAnnotation(newCMAnno);
            }
            return super.visitMethodInvocation(tree, type);
        }
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(final MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new TypesafeBuilderQualifierHierarchy(factory);
    }

    /**
     * The qualifier hierarchy is responsible for lub, glb, and subtyping between qualifiers without
     * declaratively defined subtyping relationships, like our @CalledMethods annotation.
     */
    private class TypesafeBuilderQualifierHierarchy extends MultiGraphQualifierHierarchy {
        public TypesafeBuilderQualifierHierarchy(final MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public AnnotationMirror getTopAnnotation(final AnnotationMirror start) {
            return TOP;
        }

        /**
         * GLB in this type system is set union of the arguments of the two annotations,
         * unless one of them is bottom, in which case the result is also bottom.
         */
        @Override
        public AnnotationMirror greatestLowerBound(final AnnotationMirror a1, final AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, BOTTOM) || AnnotationUtils.areSame(a2, BOTTOM)) {
                return BOTTOM;
            }
            if (!AnnotationUtils.hasElementValue(a1, "value")) {
                return a2;
            }

            if (!AnnotationUtils.hasElementValue(a2, "value")) {
                return a1;
            }

            Set<String> a1Val = getValueOfAnnotationWithStringArgument(a1).stream().collect(Collectors.toSet());
            Set<String> a2Val = getValueOfAnnotationWithStringArgument(a2).stream().collect(Collectors.toSet());
            a1Val.addAll(a2Val);
            return createCalledMethods(a1Val.toArray(new String[0]));
        }

        /**
         * LUB in this type system is set intersection of the arguments of the two annotations,
         * unless one of them is bottom, in which case the result is the other annotation.
         */
        @Override
        public AnnotationMirror leastUpperBound(final AnnotationMirror a1, final AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, BOTTOM)) {
                return a2;
            }  else if (AnnotationUtils.areSame(a2, BOTTOM)) {
                return a1;
            }
            Set<String> a1Val = getValueOfAnnotationWithStringArgument(a1).stream().collect(Collectors.toSet());
            Set<String> a2Val = getValueOfAnnotationWithStringArgument(a2).stream().collect(Collectors.toSet());
            a1Val.retainAll(a2Val);
            return createCalledMethods(a1Val.toArray(new String[0]));
        }

        /**
         * isSubtype in this type system is subset
         */
        @Override
        public boolean isSubtype(final AnnotationMirror subAnno, final AnnotationMirror superAnno) {
            if (AnnotationUtils.areSame(subAnno, BOTTOM)) {
                return true;
            } else if (AnnotationUtils.areSame(superAnno, BOTTOM)) {
                return false;
            }
            Set<String> subVal = getValueOfAnnotationWithStringArgument(subAnno).stream().collect(Collectors.toSet());
            Set<String> superVal = getValueOfAnnotationWithStringArgument(superAnno).stream().collect(Collectors.toSet());
            return subVal.containsAll(superVal);
        }
    }

    /**
     * Gets the value field of an annotation with a list of strings in its value field.
     * The empty list is returned if no value field is defined.
     */
    public static List<String> getValueOfAnnotationWithStringArgument(final AnnotationMirror anno) {
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return Collections.emptyList();
        }
        return AnnotationUtils.getElementValueArray(anno, "value", String.class, true);
    }
}
