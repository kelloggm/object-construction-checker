package org.checkerframework.checker.returnsrcvr;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class ReturnsRcvrVisitor extends BaseTypeVisitor<ReturnsRcvrAnnotatedTypeFactory> {

  public ReturnsRcvrVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitAnnotation(AnnotationTree node, Void p) {
    AnnotationMirror annot = TreeUtils.annotationFromAnnotationTree(node);
    AnnotationMirror thisAnnot = getTypeFactory().THIS_ANNOT;
    if (AnnotationUtils.areSame(annot, thisAnnot)) {
      TreePath currentPath = getCurrentPath();
      TreePath parentPath = currentPath.getParentPath();
      Tree parent = parentPath.getLeaf();
      Tree grandparent = parentPath.getParentPath().getLeaf();
      boolean isReturnAnnot =
          grandparent instanceof MethodTree
              && (parent.equals(((MethodTree) grandparent).getReturnType())
                  || parent instanceof ModifiersTree);
      boolean isCastAnnot =
          grandparent instanceof TypeCastTree
              && parent.equals(((TypeCastTree) grandparent).getType());
      if (!(isReturnAnnot || isCastAnnot)) {
        checker.report(Result.failure("invalid.this.location"), node);
      }
    }
    return super.visitAnnotation(node, p);
  }
}
