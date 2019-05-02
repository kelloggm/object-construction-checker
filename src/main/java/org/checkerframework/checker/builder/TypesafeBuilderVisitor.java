package org.checkerframework.checker.builder;

import com.sun.source.tree.AnnotationTree;
import org.checkerframework.checker.builder.qual.CalledMethodsPredicate;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.AnnotationMirror;
import java.util.ArrayList;
import java.util.Collections;

public class TypesafeBuilderVisitor extends BaseTypeVisitor<TypesafeBuilderAnnotatedTypeFactory> {
    /**
     * @param checker the type-checker associated with this visitor
     */
    public TypesafeBuilderVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Checks each @CalledMethodsPredicate annotation to make sure the predicate is well-formed.
     */
    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(node);
        if (AnnotationUtils.areSameByClass(anno, CalledMethodsPredicate.class)) {
            String predicate =
                    AnnotationUtils.getElementValue(anno, "value", String.class, false);

            try {
                new CalledMethodsPredicateEvaluator(Collections.emptySet()).evaluate(predicate, new ArrayList<>());
            } catch (IllegalArgumentException e) {
                checker.report(Result.failure("predicate.invalid", e.getMessage()), node);
                return null;
            }
        }
        return super.visitAnnotation(node, p);
    }
}
