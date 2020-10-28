package org.checkerframework.checker.objectconstruction;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.sun.tools.javac.util.Pair;
import org.checkerframework.checker.objectconstruction.framework.FrameworkSupport;
import org.checkerframework.checker.objectconstruction.qual.AlwaysCall;
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
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.springframework.expression.spel.SpelParseException;

import static javax.lang.model.element.ElementKind.*;

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

    if (!isAssignedToLocal(this.getCurrentPath())
        && !atypeFactory.returnsThis(node)
        && ((atypeFactory.transferOwnershipAtReturn && !hasNotOwningAnno(node))
            || isTransferOwnershipAtMethodInvocation(node))) {

      TypeMirror returnType = TreeUtils.typeOf(node);

      if (atypeFactory.hasAlwaysCall(returnType)) {
        if (isAssignedToFieldwithOwning(this.getCurrentPath())) {
          checkOwningFeild(this.getCurrentPath(), node);
        } else {

          String alwaysCallAnnoVal = getAlwaysCallValue(returnType);
          AnnotationMirror dummyCMAnno = atypeFactory.createCalledMethods(alwaysCallAnnoVal);
          AnnotatedTypeMirror annoType = atypeFactory.getAnnotatedType(node);
          AnnotationMirror cmAnno = annoType.getAnnotationInHierarchy(atypeFactory.TOP);

          if (!atypeFactory.getQualifierHierarchy().isSubtype(cmAnno, dummyCMAnno)) {
            checker.reportError(
                    node, "missing.alwayscall", returnType.toString(), "never assigned to a variable");
          }
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
    if (!isAssignedToLocal(this.getCurrentPath())) {
      TypeMirror type = TreeUtils.typeOf(node);
      if (atypeFactory.hasAlwaysCall(type)) {
        if (isAssignedToFieldwithOwning(this.getCurrentPath())) {
          checkOwningFeild(this.getCurrentPath(), node);
        } else {
          checker.reportError(
                  node, "missing.alwayscall", type.toString(), "never assigned to a variable");
        }
      }
    }
    return super.visitNewClass(node, p);
  }

  private String getAlwaysCallValue(TypeMirror type) {
    TypeElement eType = TypesUtils.getTypeElement(type);
    AnnotationMirror alwaysCallAnno = atypeFactory.getDeclAnnotation(eType, AlwaysCall.class);

    return (alwaysCallAnno != null)
        ? AnnotationUtils.getElementValue(alwaysCallAnno, "value", String.class, false)
        : null;
  }

  private boolean isAssignedToFieldwithOwning(final TreePath treePath) {
    Element lhsElement = getLeftHandSideOfAssign(treePath, null);
    if (lhsElement!=null && lhsElement.getKind().equals(FIELD)) {
      return (atypeFactory.getDeclAnnotation(lhsElement, Owning.class) != null);
    }

    return false;
  }

  private Element getLeftHandSideOfAssign(final TreePath treePath, Element element) {
    TreePath parentPath = treePath.getParentPath();
    Element lhsElement = element;

    if (parentPath == null) {
      return null;
    }

    Tree parent = parentPath.getLeaf();

    switch (parent.getKind()) {
      case PARENTHESIZED:
      case TYPE_CAST:
        return getLeftHandSideOfAssign(parentPath, lhsElement);
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree cet = (ConditionalExpressionTree) parent;
        if (cet.getCondition() == treePath.getLeaf()) {
          // The assignment context for the condition is simply boolean.
          // No point in going on.
          return lhsElement;
        }
        // Otherwise use the context of the ConditionalExpressionTree.
        return getLeftHandSideOfAssign(parentPath, lhsElement);
      case ASSIGNMENT: // check if the left hand is a local variable
        final JCTree.JCExpression lhs = ((JCTree.JCAssign) parent).lhs;
        lhsElement = TreeUtils.elementFromTree((lhs).getTree());
        return lhsElement;
        default:
          return lhsElement;
    }
  }

  private void checkOwningFeild(final TreePath treePath, Tree node) {
    Element fieldElement = getLeftHandSideOfAssign(treePath, null);
    Element enclosingElemnt =  fieldElement.getEnclosingElement();
    String fieldElAnno = atypeFactory.getAlwaysCallValue(fieldElement);
    String enclosingElAnno = atypeFactory.getAlwaysCallValue(enclosingElemnt);
    boolean report = true;
    if (enclosingElAnno != null) {
      List<? extends Element> classElements = enclosingElemnt.getEnclosedElements();
      for (Element element : classElements) {
        if (element.getKind().equals(METHOD)) {
          if (element.getSimpleName().toString().equals(enclosingElAnno)) {
            AnnotationMirror annotationMirror = atypeFactory.getDeclAnnotation(element, EnsuresCalledMethods.class);
            if (annotationMirror == null) {
              break;
            }
            List<Pair<Symbol.MethodSymbol,Attribute>> list =  ((Attribute.Compound) annotationMirror).values;
            if (list.get(0).snd.getValue().toString().contains(fieldElement.getSimpleName().toString())) {
              if (((Attribute.Array) list.get(1).snd).getValue().head.getValue().equals(fieldElAnno)) {
                report = false;
              }
            }
            break;
          }
        }
      }
    }

    if (report) {
      checker.reportError(
              node, "missing.alwayscall", TreeUtils.typeOf(node).toString(), "field might not be safe");
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
      case ASSIGNMENT: // check if the left hand is a local variable
        Element lhsElement = getLeftHandSideOfAssign(treePath, null);
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
