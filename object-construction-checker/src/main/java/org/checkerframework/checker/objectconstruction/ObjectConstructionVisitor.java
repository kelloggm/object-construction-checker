package org.checkerframework.checker.objectconstruction;

import static org.checkerframework.checker.objectconstruction.ObjectConstructionAnnotatedTypeFactory.getValueOfAnnotationWithStringArgument;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.objectconstruction.framework.FrameworkSupport;
import org.checkerframework.checker.objectconstruction.qual.AlwaysCall;
import org.checkerframework.checker.objectconstruction.qual.CalledMethodsPredicate;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.springframework.expression.spel.SpelParseException;

public class ObjectConstructionVisitor
    extends BaseTypeVisitor<ObjectConstructionAnnotatedTypeFactory> {
  private Map<MethodTree, List<LocalVariableNode>> methodVariables;

  /** @param checker the type-checker associated with this visitor */
  public ObjectConstructionVisitor(final BaseTypeChecker checker) {
    super(checker);
    methodVariables = new HashMap<>();
  }

  /** Checks each @CalledMethodsPredicate annotation to make sure the predicate is well-formed. */
  @Override
  public Void visitAnnotation(final AnnotationTree node, final Void p) {
    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(node);
    if (AnnotationUtils.areSameByClass(anno, CalledMethodsPredicate.class)) {
      String predicate = AnnotationUtils.getElementValue(anno, "value", String.class, false);

      try {
        new CalledMethodsPredicateEvaluator(Collections.emptyList()).evaluate(predicate);
      } catch (SpelParseException e) {
        checker.report(Result.failure("predicate.invalid", e.getMessage()), node);
        return null;
      }
    }
    return super.visitAnnotation(node, p);
  }

  @Override
  public Void visitVariable(VariableTree node, Void o) {
    TreePath path = this.getCurrentPath();
    Tree enclosingTree = TreeUtils.enclosingOfKind(path, Tree.Kind.METHOD);

    if (enclosingTree != null && enclosingTree.getKind() == Tree.Kind.METHOD) {

      Element element = TreeUtils.elementFromDeclaration(node);
      TypeMirror type = element.asType();
      TypeElement eType = TypesUtils.getTypeElement(type);
      LocalVariableNode localVariableNode = new LocalVariableNode(node);

      if (hasAlwaysCall(eType) && methodVariables.get(enclosingTree)!=null) {
        methodVariables.get(enclosingTree).add(localVariableNode);
      }
    }
    return super.visitVariable(node, o);
  }

  @Override
  public Void visitMethod(MethodTree node, Void o) {

    TreePath path = this.getCurrentPath();
    if (methodVariables.get(node) == null) {
      methodVariables.put(node, new ArrayList<>());
    }
    Void v = super.visitMethod(node, o);

    for (LocalVariableNode localVariableNode : methodVariables.get(node)) {

      Element element = localVariableNode.getElement();
      Store regularExitStore = atypeFactory.getRegularExitStore(node);
      CFValue cfValue = ((CFStore) regularExitStore).getValue(localVariableNode);

      if (cfValue != null) {
        Set<AnnotationMirror> annotationMirrors = cfValue.getAnnotations();

        for (AnnotationMirror annoMirror : annotationMirrors) {
          List<String> annoValues = getValueOfAnnotationWithStringArgument(annoMirror);
          alwaysCallCheckAtRegularExitPoint(annoValues, element);
        }

      }
    }

//          LocalVariablesVisitor localvarvisitor = new LocalVariablesVisitor();
//        localvarvisitor.setRoot(node);
    //      node.accept(localvarvisitor, o);


    //    exitStore.get
    //
    // ((CFStore)((AbstractMap.SimpleEntry)atypeFactory.regularExitStores.entrySet().toArray()[2]).getValue()).localVariableValues
    //    LocalVariableNode lvn = ((CFStore)regularExitStore).getValue();

    return v;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

    if (!isAssignedToLocal(this.getCurrentPath())) {
      ExecutableElement element = TreeUtils.elementFromUse(node);
      TypeMirror returnType = element.getReturnType();
      TypeElement eType = TypesUtils.getTypeElement(returnType);

      if (hasAlwaysCall(eType)) {
        AnnotationMirror alwaysCallAnno = atypeFactory.getDeclAnnotation(eType, AlwaysCall.class);
        String alwaysCallAnnoVal =
            AnnotationUtils.getElementValue(alwaysCallAnno, "value", String.class, false);
        List<String> currentCalledMethods = getCalledMethodAnnotation(node);

        if (!currentCalledMethods.contains(alwaysCallAnnoVal)) {
          checker.report(
              Result.failure(
                  "calledMethod doesn't contain alwaysCall obligations", element.getSimpleName()),
              node);
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

  public List<String> getCalledMethodAnnotation(MethodInvocationTree node) {
    AnnotationMirror calledMethodAnno;
    AnnotatedTypeMirror currentType = getTypeFactory().getAnnotatedType(node);

    if (currentType == null || !currentType.isAnnotatedInHierarchy(atypeFactory.TOP)) {
      calledMethodAnno = atypeFactory.TOP;
    } else {
      calledMethodAnno = currentType.getAnnotationInHierarchy(atypeFactory.TOP);
    }

    List<String> currentCalledMethods = getValueOfAnnotationWithStringArgument(calledMethodAnno);
    return currentCalledMethods;
  }

  public static boolean isAssignedToLocal(final TreePath treePath) {
    TreePath parentPath = treePath.getParentPath();

    if (parentPath == null) {
      return false;
    }

    Tree parent = parentPath.getLeaf();
    switch (parent.getKind()) {
      case PARENTHESIZED:
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
      case RETURN:
      case VARIABLE:
        return true;
      default:
        return false;
    }
  }

  public boolean hasAlwaysCall(TypeElement type) {

    if (type != null && atypeFactory.getDeclAnnotation(type, AlwaysCall.class) != null) {
      return true;
    } else {
      return false;
    }
  }

  public void alwaysCallCheckAtRegularExitPoint(List<String> annoValues, Element element) {
    boolean exist = false;
    String allwaysCallAnooValue = getAlwaysCallValue(element);

    for (String annoValue : annoValues) {
      if (annoValue.equals(allwaysCallAnooValue)) {
        exist = true;
        break;
      }
    }

    if (!exist) {
      checker.report(
          Result.failure(
                  "missing.alwayscall",
              "Method "
                  + allwaysCallAnooValue
                  + "() has not been called on local variable "
                  + element.getSimpleName(),
              element.getSimpleName()),
          element);
    }
  }

  public String getAlwaysCallValue(Element element){
    TypeMirror type = element.asType();
    TypeElement eType = TypesUtils.getTypeElement(type);
    AnnotationMirror allwaysCallAnoo = atypeFactory.getDeclAnnotation(eType, AlwaysCall.class);
    String allwaysCallAnooValue =
            AnnotationUtils.getElementValue(allwaysCallAnoo, "value", String.class, false);
    return allwaysCallAnooValue;
  }

  //    private class LocalVariablesVisitor extends TreePathScanner {
  //
  //    List<Element> methodVariables= new ArrayList<>();
  //
  //      public LocalVariablesVisitor(){
  //
  //
  //      }
  //
  //      @Override
  //      public Object visitVariable(VariableTree node, Object o) {
  //        TreePath path = this.getCurrentPath();
  //        Tree enclosingTree = TreeUtils.enclosingOfKind(path, Tree.Kind.METHOD);
  //
  //        if (enclosingTree != null && enclosingTree.getKind() == Tree.Kind.METHOD) {
  //          Element element = TreeUtils.elementFromDeclaration(node);
  //          TypeMirror type = element.asType();
  //
  //          TypeElement eType = TypesUtils.getTypeElement(type);
  //
  //          if (hasAlwaysCall(eType)) {
  //            methodVariables.add(element);
  //          }
  //        }
  //        return super.visitVariable(node, o);
  //      }
  //
  //      public void addVariable(MethodTree methodtree, VariableTree variableTree){
  //
  //      }
  //
  //
  //
  //      public void getAllVariable(){
  //
  //      }
  //
  //      public boolean hasAlwaysCall(TypeElement type) {
  //        if (type != null && atypeFactory.getDeclAnnotation(type, AlwaysCall.class) != null) {
  //          return true;
  //        } else {
  //          return false;
  //        }
  //      }
  //    }

}
