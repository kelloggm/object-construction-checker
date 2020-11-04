package org.checkerframework.checker.objectconstruction;

import static javax.lang.model.element.ElementKind.*;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.checkerframework.checker.objectconstruction.framework.FrameworkSupport;
import org.checkerframework.checker.objectconstruction.qual.CalledMethods;
import org.checkerframework.checker.objectconstruction.qual.CalledMethodsPredicate;
import org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethods;
import org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.checker.objectconstruction.qual.NotOwning;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.springframework.expression.spel.SpelParseException;

public class ObjectConstructionVisitor
    extends BaseTypeVisitor<ObjectConstructionAnnotatedTypeFactory> {

  /** @param checker the type-checker associated with this visitor */
  public ObjectConstructionVisitor(final BaseTypeChecker checker) {
    super(checker);
  }

  /** Checks each @CalledMethodsPredicate annotation to make sure the predicate is well-formed. */
  @Override
  public Void visitAnnotation(final AnnotationTree node, final Void p) {
    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(node);
    if (AnnotationUtils.areSameByClass(anno, CalledMethodsPredicate.class)) {
      String predicate = AnnotationUtils.getElementValue(anno, "value", String.class, false);

      try {
        CalledMethodsPredicateEvaluator.evaluate(predicate, Collections.emptyList());
      } catch (SpelParseException e) {
        checker.report(
            node, new DiagMessage(Diagnostic.Kind.ERROR, "predicate.invalid", e.getMessage()));
        return null;
      }
    } else if (AnnotationUtils.areSameByClass(anno, EnsuresCalledMethodsVarArgs.class)) {
      // we can't verify these yet.  emit an error (which will have to be suppressed) for now
      checker.report(node, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.unverified"));
      return null;
    }
    return super.visitAnnotation(node, p);
  }

  @Override
  public Void visitMethod(MethodTree node, Void p) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(node);
    AnnotationMirror annot = atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethodsVarArgs.class);
    if (annot != null) {
      if (!elt.isVarArgs()) {
        checker.report(
            node, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.annotation.invalid"));
        return null;
      }
    }
    return super.visitMethod(node, p);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

    if (checker.hasOption(ObjectConstructionChecker.CHECK_MUST_CALL)
        && !isAssignedToLocal(this.getCurrentPath())
        && !atypeFactory.returnsThis(node)
        && ((atypeFactory.transferOwnershipAtReturn && !hasNotOwningAnno(node))
            || isTransferOwnershipAtMethodInvocation(node))) {

      // Calls to super() can be disregarded; the object under construction should inherit
      // the MustCall responsibility of its super class.
      if (!TreeUtils.isSuperConstructorCall(node) && atypeFactory.hasMustCall(node)) {
        TypeMirror returnType = TreeUtils.typeOf(node);
        List<String> mustCallAnnoVal = atypeFactory.getMustCallValue(node);
        AnnotationMirror dummyCMAnno =
            atypeFactory.createCalledMethods(mustCallAnnoVal.toArray(new String[0]));
        AnnotatedTypeMirror annoType = atypeFactory.getAnnotatedType(node);
        AnnotationMirror cmAnno = annoType.getAnnotationInHierarchy(atypeFactory.TOP);

        if (!atypeFactory.getQualifierHierarchy().isSubtype(cmAnno, dummyCMAnno)) {
          checker.reportError(
              node,
              "required.method.not.called",
              atypeFactory.formatMissingMustCallMethods(mustCallAnnoVal),
              returnType.toString(),
              "never assigned to a variable");
        }
      }
    }

    if (checker.getBooleanOption(ObjectConstructionChecker.COUNT_FRAMEWORK_BUILD_CALLS)) {
      ExecutableElement element = TreeUtils.elementFromUse(node);
      for (FrameworkSupport frameworkSupport : getTypeFactory().getFrameworkSupports()) {
        if (frameworkSupport.isBuilderBuildMethod(element)) {
          ((ObjectConstructionChecker) checker).numBuildCalls++;
          break;
        }
      }
    }
    return super.visitMethodInvocation(node, p);
  }

  boolean isTransferOwnershipAtMethodInvocation(MethodInvocationTree tree) {
    ExecutableElement ee = TreeUtils.elementFromUse(tree);
    return (atypeFactory.getDeclAnnotation(ee, Owning.class) != null);
  }

  boolean hasNotOwningAnno(MethodInvocationTree tree) {
    ExecutableElement ee = TreeUtils.elementFromUse(tree);
    return (atypeFactory.getDeclAnnotation(ee, NotOwning.class) != null);
  }

  @Override
  public Void visitNewClass(NewClassTree node, Void p) {
    if (checker.hasOption(ObjectConstructionChecker.CHECK_MUST_CALL)
        && !isAssignedToLocal(this.getCurrentPath())) {
      List<String> mustCallVal = atypeFactory.getMustCallValue(node);
      if (!mustCallVal.isEmpty()) {
        TypeMirror type = TreeUtils.typeOf(node);
        checker.reportError(
            node,
            "required.method.not.called",
            atypeFactory.formatMissingMustCallMethods(mustCallVal),
            type.toString(),
            "never assigned to a variable");
      }
    }
    return super.visitNewClass(node, p);
  }

  @Override
  public Void visitVariable(VariableTree node, Void p) {
    Element varElement = TreeUtils.elementFromTree(node);

    if (varElement.getKind().isField()
        && atypeFactory.getDeclAnnotation(varElement, Owning.class) != null) {
      List<String> fieldMCAnno = atypeFactory.getMustCallValue(varElement);
      if (ElementUtils.isFinal(varElement)) {
        checkFinalOwningField(varElement);
      } else if (!fieldMCAnno.isEmpty()) {
        List<String> varMCValue = atypeFactory.getMustCallValue(varElement);
        checker.reportError(
            node,
            "required.method.not.called",
            atypeFactory.formatMissingMustCallMethods(varMCValue),
            varElement.asType().toString(),
            " Non-final owning field might be overwritten");
      }
    }

    return super.visitVariable(node, p);
  }

  private void checkFinalOwningField(Element field) {
    List<String> fieldMCAnno = atypeFactory.getMustCallValue(field);
    String error = "";

    if (!fieldMCAnno.isEmpty()) {
      Element enclosingElement = field.getEnclosingElement();
      List<String> enclosingMCAnno = atypeFactory.getMustCallValue(enclosingElement);

      if (enclosingMCAnno != null) {
        List<? extends Element> classElements = enclosingElement.getEnclosedElements();
        for (Element element : classElements) {
          if (fieldMCAnno.isEmpty()) {
            return;
          }
          if (element.getKind().equals(METHOD)
              && enclosingMCAnno.contains(element.getSimpleName().toString())) {
            AnnotationMirror ensuresCalledMethodsAnno =
                atypeFactory.getDeclAnnotation(element, EnsuresCalledMethods.class);

            if (ensuresCalledMethodsAnno != null) {
              List<String> values =
                  ValueCheckerUtils.getValueOfAnnotationWithStringArgument(
                      ensuresCalledMethodsAnno);
              if (values.stream()
                  .anyMatch(value -> value.contains(field.getSimpleName().toString()))) {
                List<String> methods =
                    AnnotationUtils.getElementValueArray(
                        ensuresCalledMethodsAnno, "methods", String.class, false);
                fieldMCAnno.removeAll(methods);
              }
            }

            if (!fieldMCAnno.isEmpty()) {
              error =
                  " @EnsuresCalledMethods written on MustCall methods doesn't contain "
                      + atypeFactory.formatMissingMustCallMethods(fieldMCAnno);
            }
          }
        }
      } else {
        error = " The enclosing element doesn't have a @MustCall annotation";
      }
    }

    if (!fieldMCAnno.isEmpty()) {
      checker.reportError(
          field,
          "required.method.not.called",
          atypeFactory.formatMissingMustCallMethods(fieldMCAnno),
          field.asType().toString(),
          error);
    }
  }

  private boolean isAssignedToLocal(final TreePath treePath) {
    TreePath parentPath = treePath.getParentPath();
    if (parentPath == null) {
      return false;
    }

    Tree parent = parentPath.getLeaf();
    switch (parent.getKind()) {
      case PARENTHESIZED:
      case TYPE_CAST:
        return isAssignedToLocal(parentPath);
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree cet = (ConditionalExpressionTree) parent;
        if (cet.getCondition() == treePath.getLeaf()) {
          // The assignment context for the condition is simply boolean.
          // No point in going on.
          return false;
        }
        // Otherwise use the context of the ConditionalExpressionTree.
        return isAssignedToLocal(parentPath);
      case ASSIGNMENT: // check if the left hand is a local variable or an owning field
        final JCTree.JCExpression lhs = ((JCTree.JCAssign) parent).lhs;
        Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());
        if (lhsElement.getKind().equals(FIELD)) {
          return (atypeFactory.getDeclAnnotation(lhsElement, Owning.class) != null);
        }
        return lhsElement.getKind().equals(LOCAL_VARIABLE);
      case VARIABLE:
      case METHOD_INVOCATION:
      case NEW_CLASS:
      case RETURN:
        return true;
      default:
        return false;
    }
  }

  /**
   * Adds special reporting for method.invocation.invalid errors to turn them into
   * finalizer.invocation.invalid errors.
   */
  @Override
  protected void reportMethodInvocabilityError(
      MethodInvocationTree node, AnnotatedTypeMirror found, AnnotatedTypeMirror expected) {

    AnnotationMirror expectedCM = expected.getAnnotation(CalledMethods.class);
    if (expectedCM != null) {
      AnnotationMirror foundCM = found.getAnnotation(CalledMethods.class);
      Set<String> foundMethods =
          foundCM == null
              ? Collections.emptySet()
              : new HashSet<>(ValueCheckerUtils.getValueOfAnnotationWithStringArgument(foundCM));
      List<String> expectedMethods =
          ValueCheckerUtils.getValueOfAnnotationWithStringArgument(expectedCM);
      StringBuilder missingMethods = new StringBuilder();
      for (String expectedMethod : expectedMethods) {
        if (!foundMethods.contains(expectedMethod)) {
          missingMethods.append(expectedMethod);
          missingMethods.append("() ");
        }
      }

      checker.reportError(node, "finalizer.invocation.invalid", missingMethods.toString());
    } else {
      super.reportMethodInvocabilityError(node, found, expected);
    }
  }
}
