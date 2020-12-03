package org.checkerframework.checker.unconnectedsocket;

import com.sun.source.tree.VariableTree;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class UnconnectedSocketVisitor
    extends BaseTypeVisitor<UnconnectedSocketAnnotatedTypeFactory> {
  public UnconnectedSocketVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitVariable(VariableTree node, Void p) {
    VariableElement decl = TreeUtils.elementFromDeclaration(node);
    if (decl.getKind() == ElementKind.FIELD) {
      AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
      if (!type.hasAnnotation(atypeFactory.TOP)) {
        checker.reportError(node, "unconnected.field");
      }
    }
    return super.visitVariable(node, p);
  }
}
