package org.checkerframework.checker.builder;

import com.sun.source.tree.AnnotationTree;
import org.checkerframework.checker.builder.qual.CalledMethodsPredicate;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.springframework.expression.spel.SpelParseException;

import javax.lang.model.element.AnnotationMirror;
import java.util.Collections;

public class TypesafeBuilderVisitor extends BaseTypeVisitor<TypesafeBuilderAnnotatedTypeFactory> {
    /**
     * @param checker the type-checker associated with this visitor
     */
    public TypesafeBuilderVisitor(final BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Checks each @CalledMethodsPredicate annotation to make sure the predicate is well-formed.
     */
    @Override
    public Void visitAnnotation(final AnnotationTree node, final Void p) {
        AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(node);
        if (AnnotationUtils.areSameByClass(anno, CalledMethodsPredicate.class)) {
            String predicate =
                    AnnotationUtils.getElementValue(anno, "value", String.class, false);

            try {
                new CalledMethodsPredicateEvaluator(Collections.emptySet()).evaluate(predicate);
            } catch (SpelParseException e) {
                checker.report(Result.failure("predicate.invalid", e.getMessage()), node);
                return null;
            }
        }
        return super.visitAnnotation(node, p);
    }
}
