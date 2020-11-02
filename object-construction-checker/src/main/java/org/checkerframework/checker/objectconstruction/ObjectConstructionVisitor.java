package org.checkerframework.checker.objectconstruction;

import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.checkerframework.checker.calledmethods.CalledMethodsVisitor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.objectconstruction.qual.AlwaysCall;
import org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.checker.objectconstruction.qual.NotOwning;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class ObjectConstructionVisitor extends CalledMethodsVisitor {

  private @MonotonicNonNull ObjectConstructionAnnotatedTypeFactory atypeFactory;

  /** @param checker the type-checker associated with this visitor */
  public ObjectConstructionVisitor(final BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  protected ObjectConstructionAnnotatedTypeFactory createTypeFactory() {
    atypeFactory = new ObjectConstructionAnnotatedTypeFactory(checker);
    return atypeFactory;
  }

  /**
   * Issue an error at every EnsuresCalledMethodsVarArgs annotation, because using it is unsound.
   */
  @Override
  public Void visitAnnotation(final AnnotationTree node, final Void p) {
    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(node);
    if (AnnotationUtils.areSameByClass(anno, EnsuresCalledMethodsVarArgs.class)) {
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
        String alwaysCallAnnoVal = getAlwaysCallValue(returnType);
        AnnotationMirror dummyCMAnno = atypeFactory.createCalledMethods(alwaysCallAnnoVal);
        AnnotatedTypeMirror annoType = atypeFactory.getAnnotatedType(node);
        AnnotationMirror cmAnno = annoType.getAnnotationInHierarchy(atypeFactory.top);

        if (!atypeFactory.getQualifierHierarchy().isSubtype(cmAnno, dummyCMAnno)) {
          checker.reportError(
              node, "missing.alwayscall", returnType.toString(), "never assigned to a variable");
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
        checker.reportError(
            node, "missing.alwayscall", type.toString(), "never assigned to a variable");
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
        final JCTree.JCExpression lhs = ((JCTree.JCAssign) parent).lhs;
        return (lhs instanceof JCTree.JCIdent)
            ? (((JCTree.JCIdent) lhs).sym.getKind().equals(LOCAL_VARIABLE))
            : false;
      case METHOD_INVOCATION:
      case NEW_CLASS:
      case RETURN:
      case VARIABLE:
        return true;
      default:
        return false;
    }
  }
}
