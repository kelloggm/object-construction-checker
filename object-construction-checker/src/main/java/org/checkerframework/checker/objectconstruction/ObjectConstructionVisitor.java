package org.checkerframework.checker.objectconstruction;

import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;
import static javax.lang.model.type.TypeKind.VOID;
import static org.checkerframework.checker.objectconstruction.ObjectConstructionAnnotatedTypeFactory.getValueOfAnnotationWithStringArgument;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.checker.objectconstruction.framework.FrameworkSupport;
import org.checkerframework.checker.objectconstruction.qual.AlwaysCall;
import org.checkerframework.checker.objectconstruction.qual.CalledMethods;
import org.checkerframework.checker.objectconstruction.qual.CalledMethodsPredicate;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.org.objectweb.asmx.tree.analysis.Value;
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
        new CalledMethodsPredicateEvaluator(Collections.emptyList()).evaluate(predicate);
      } catch (SpelParseException e) {
        checker.report(
            node, new DiagMessage(Diagnostic.Kind.ERROR, "predicate.invalid", e.getMessage()));
        return null;
      }
    }
    return super.visitAnnotation(node, p);
  }

  @Override
  public Void visitMethod(MethodTree node, Void o) {

    if(node.getBody()!=null) {
      LocalVariablesVisitor localVarVisitor = new LocalVariablesVisitor(node);
      localVarVisitor.scan(this.getCurrentPath(), o);
      localVarVisitor.checksForExitPoints();
    }
    return super.visitMethod(node, o);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

    if (!isAssignedToLocal(this.getCurrentPath()) && !atypeFactory.returnsThis(node)) {
      ExecutableElement exeElement = TreeUtils.elementFromUse(node);
      TypeMirror returnType = exeElement.getReturnType();

//
//
//      AnnotatedTypeMirror methodATm = atypeFactory.getAnnotatedType(exeElement);
//      AnnotatedTypeMirror rType =
//              ((AnnotatedTypeMirror.AnnotatedExecutableType) methodATm).getReturnType();

      if (hasAlwaysCall(returnType)) {
        TypeElement eType = TypesUtils.getTypeElement(returnType);
        AnnotationMirror alwaysCallAnno = atypeFactory.getDeclAnnotation(eType, AlwaysCall.class);
        String alwaysCallAnnoVal =
            AnnotationUtils.getElementValue(alwaysCallAnno, "value", String.class, false);
        List<String> currentCalledMethods = getCalledMethodAnnotation(node);

        if (!currentCalledMethods.contains(alwaysCallAnnoVal)) {
          String error = " " + alwaysCallAnnoVal + " has not been called";
          checker.report(node, new DiagMessage(Diagnostic.Kind.ERROR, "missing.alwayscall", error));
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

  @Override
  public Void visitNewClass(NewClassTree node, Void p) {

    if(!isAssignedToLocal(this.getCurrentPath())){

      AnnotatedTypeFactory.ParameterizedExecutableType ptype = atypeFactory.constructorFromUse(node);
      AnnotatedTypeMirror.AnnotatedExecutableType constructor = ptype.executableType;
      ExecutableElement ee = constructor.getElement();
      TypeMirror type = ((Symbol.ClassSymbol)((Symbol.MethodSymbol)ee).owner).type;

      if (hasAlwaysCall(type))
//      String error = " " + alwaysCallValue + " has not been called";
        checker.report(node, new DiagMessage(Diagnostic.Kind.ERROR, "missing.alwayscall", " "));
    }
    return super.visitNewClass(node, p);
  }


  private List<String> getCalledMethodAnnotation(MethodInvocationTree node) {
    AnnotationMirror calledMethodAnno;
    AnnotatedTypeMirror currentType = getTypeFactory().getAnnotatedType(node);

    if (currentType == null || !currentType.isAnnotatedInHierarchy(atypeFactory.TOP)) {
      calledMethodAnno = atypeFactory.TOP;
    } else {
      calledMethodAnno = currentType.getAnnotationInHierarchy(atypeFactory.TOP);
    }

    return getValueOfAnnotationWithStringArgument(calledMethodAnno);
  }

  private static boolean isAssignedToLocal(final TreePath treePath) {
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
        final JCTree.JCExpression lhs = ((JCTree.JCAssign) parent).lhs;
        return (((JCTree.JCIdent)lhs).sym.getKind() == LOCAL_VARIABLE);

      case RETURN:
      case VARIABLE:
        return true;
      default:
        return false;
    }
  }

  private boolean hasAlwaysCall(TypeMirror type) {
    TypeElement eType = TypesUtils.getTypeElement(type);
    if (eType == null) {
      return false;
    } else {
      return (atypeFactory.getDeclAnnotation(eType, AlwaysCall.class) != null);
    }
  }

  private String getAlwaysCallValue(Element element) {

    TypeMirror type = element.asType();
    TypeElement eType = TypesUtils.getTypeElement(type);
    AnnotationMirror alwaysCallAnnotation = atypeFactory.getDeclAnnotation(eType, AlwaysCall.class);

    if (alwaysCallAnnotation != null) {
      return AnnotationUtils.getElementValue(alwaysCallAnnotation, "value", String.class, false);
    } else {
      return null;
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

  /** This class is needed to visit all local variables of each method. */
  private class LocalVariablesVisitor extends TreePathScanner {

    // Keeps a list of all local variable nodes that have @AlwaysCall annotation
    List<LocalVariableNode> methodVariablesList;
    MethodTree node;
    List<Pair<ReturnNode, TransferResult<CFValue, CFStore>>> returnStatementStore;

    private LocalVariablesVisitor(MethodTree node) {
      this.node = node;
      methodVariablesList = new ArrayList<>();

    }

    /**
     * Creates a local variable node and add it to the methodVariablesList, if the variable is not
     * formal parameter and its type has alwaysCall annotation.
     */
    @Override
    public Object visitVariable(VariableTree node, Object o) {

      Element element = TreeUtils.elementFromDeclaration(node);
      TypeMirror type = element.asType();

      if (hasAlwaysCall(type) && !isFormalParameter(node)) {
        LocalVariableNode localVariableNode = new LocalVariableNode(node);
        methodVariablesList.add(localVariableNode);
      }

      return super.visitVariable(node, o);
    }

    /** Checks local variables at method exit points that can be regular or exceptional */
    private void checksForExitPoints() {
      checkReturnStatementStore();

      checkStoreAtRegularExitPoint();
//      checkStoreAtExceptionalExitPoint();
    }



    private void checkReturnStatementStore(){
      returnStatementStore = atypeFactory.getReturnStatementStores(node);

      for (Pair<ReturnNode, TransferResult<CFValue, CFStore>> exitStore : returnStatementStore){

        if (exitStore.second.getRegularStore()!=null){
          CFStore result = exitStore.second.getRegularStore();

          for (LocalVariableNode localvariableNode: methodVariablesList){
            if (result.getValue(localvariableNode) != null){

              CFValue cfValue = result.getValue(localvariableNode);
              reportAlwaysCallExitPointsErrors(localvariableNode, cfValue);
            }
          }
        }
      }
    }


    /**
     * Iterates over methodVariablesList and the finds abstract value of each local variable node at
     * the regular exit point. Then, by passing local variable nodes and their abstract values to
     * the reportAlwaysCallExitPointsErrors method, it checks if required functions are called on
     * each local variable node or not.
     */
    private void checkStoreAtRegularExitPoint() {
      Store regularExitStore = atypeFactory.getRegularExitStore(node);

      if (regularExitStore != null) {

        for (LocalVariableNode localVariableNode : methodVariablesList) {
          CFValue cfValue = ((CFStore) regularExitStore).getValue(localVariableNode);

          if (cfValue != null) {
            reportAlwaysCallExitPointsErrors(localVariableNode, cfValue);
          }
        }
      }
    }

    private void checkStoreAtExceptionalExitPoint() {
      Store exceptionalExitStore = atypeFactory.getExceptionalExitStore(node);

      if (exceptionalExitStore != null) {

        for (LocalVariableNode localVariableNode : methodVariablesList) {
          CFValue cfValue = ((CFStore) exceptionalExitStore).getValue(localVariableNode);

          if (cfValue != null) {
            reportAlwaysCallExitPointsErrors(localVariableNode, cfValue);
          }
        }
      }
    }

    /**
     * Given a local variable node and its abstract value at exit points, reports an error if
     * required functions are called before the exit points.
     *
     * @param localVariableNode a local variable node that has @AlwaysCall annotation on its type
     * @param cfValue the abstract value of the localVariableNode at exit points
     */
    private void reportAlwaysCallExitPointsErrors(
        LocalVariableNode localVariableNode, CFValue cfValue) {

      Element element = localVariableNode.getElement();
      String alwaysCallValue = getAlwaysCallValue(element);
      Set<AnnotationMirror> annotationMirrors = cfValue.getAnnotations();

      for (AnnotationMirror annotationMirror : annotationMirrors) {
        List<String> annotationValues = getValueOfAnnotationWithStringArgument(annotationMirror);

        if (!annotationValues.contains(alwaysCallValue)) {
          String error = " " + alwaysCallValue + " has not been called";
          checker.report(element, new DiagMessage(Diagnostic.Kind.ERROR, "missing.alwayscall", error));
        }
      }
    }

    private boolean isFormalParameter(VariableTree var) {
      return (node.getParameters().contains(var));
    }
  }
}
