package org.checkerframework.checker.mustcall;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.qual.ResetMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.*;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionContext;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Transfer function for the must-call type system. Handles defaulting for string concatenations.
 */
public class MustCallTransfer extends CFTransfer {

  private MustCallAnnotatedTypeFactory atypeFactory;

  static HashMap<Node, LocalVariableNode> tempVars = new HashMap<>();

  public MustCallTransfer(CFAnalysis analysis) {
    super(analysis);
    atypeFactory = (MustCallAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);
    if (!atypeFactory.getChecker().hasOption(MustCallChecker.NO_ACCUMULATION_FRAMES)) {
      Set<JavaExpression> targetExprs = getResetMustCallExpressions(n, atypeFactory);
      for (JavaExpression targetExpr : targetExprs) {
        AnnotationMirror defaultType =
            atypeFactory
                .getAnnotatedType(TypesUtils.getTypeElement(targetExpr.getType()))
                .getAnnotationInHierarchy(atypeFactory.TOP);

        if (result.containsTwoStores()) {
          CFStore thenStore = result.getThenStore();
          thenStore.clearValue(targetExpr);
          thenStore.insertValue(targetExpr, defaultType);
          CFStore elseStore = result.getElseStore();
          elseStore.clearValue(targetExpr);
          elseStore.insertValue(targetExpr, defaultType);
        } else {
          CFStore store = result.getRegularStore();
          store.clearValue(targetExpr);
          store.insertValue(targetExpr, defaultType);
        }
      }
    }
    if (tempVars.containsKey(n)) {
      LocalVariableNode tempVar = tempVars.get(n);
      JavaExpression localExp = JavaExpression.fromNode(atypeFactory, tempVar);
      insertIntoStores(result, localExp, n);
      tempVars.remove(n);
    }
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitObjectCreation(
      ObjectCreationNode n, TransferInput<CFValue, CFStore> p) {
    TransferResult<CFValue, CFStore> result = super.visitObjectCreation(n, p);

    if (tempVars.containsKey(n)) {
      LocalVariableNode tempVar = tempVars.get(n);
      JavaExpression localExp = JavaExpression.fromNode(atypeFactory, tempVar);
      insertIntoStores(result, localExp, n);
      tempVars.remove(n);
    }

    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitTernaryExpression(
      TernaryExpressionNode n, TransferInput<CFValue, CFStore> p) {
    TransferResult<CFValue, CFStore> result = super.visitTernaryExpression(n, p);

    if (tempVars.containsKey(n)) {
      LocalVariableNode tempVar = tempVars.get(n);
      JavaExpression localExp = JavaExpression.fromNode(atypeFactory, tempVar);
      insertIntoStores(result, localExp, n);
      tempVars.remove(n);
    }

    return result;
  }

  private void insertIntoStores(
      TransferResult<CFValue, CFStore> result, JavaExpression target, Node node) {
    TypeElement te = TypesUtils.getTypeElement(node.getType());
    if (te == null || ((Symbol.ClassSymbol) te).type.getKind().equals(TypeKind.VOID)) {
      return;
    }
    AnnotationMirror newAnno =
        atypeFactory.getAnnotatedType(te).getAnnotationInHierarchy(atypeFactory.TOP);
    if (result.containsTwoStores()) {
      CFStore thenStore = result.getThenStore();
      CFStore elseStore = result.getElseStore();
      thenStore.insertValue(target, newAnno);
      elseStore.insertValue(target, newAnno);
    } else {
      CFStore store = result.getRegularStore();
      store.insertValue(target, newAnno);
    }
  }

  /**
   * If the given method invocation node is a ResetMustCall method, then gets the JavaExpressions
   * corresponding to the targets. If any expression is unparseable, this method uses the type
   * factory's error reporting inferface to throw an error and returns the empty set. Also return
   * the empty set if the given method is not a ResetMustCall method.
   *
   * @param n a method invocation
   * @param atypeFactory the type factory to report errors and parse the expression string
   * @return a list of JavaExpressions representing the targets, if the method is a ResetMustCall
   *     method and the targets are parseable; the empty set otherwise.
   */
  public static Set<JavaExpression> getResetMustCallExpressions(
      MethodInvocationNode n, GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
    return getResetMustCallExpressions(n, atypeFactory, null);
  }

  /**
   * If the given method invocation node is a ResetMustCall method, then gets the JavaExpressions
   * corresponding to the targets. If any expression is unparseable, this method uses the type
   * factory's error reporting inferface to throw an error and returns the empty set. Also return
   * the empty set if the given method is not a ResetMustCall method.
   *
   * @param n a method invocation
   * @param atypeFactory the type factory to report errors and parse the expression string
   * @param currentPath the path to n, if it is already available, to avoid potentially-expensive
   *     recomputation. If null, the path will be computed.
   * @return a list of JavaExpressions representing the targets, if the method is a ResetMustCall
   *     method and the targets are parseable; the empty set otherwise.
   */
  public static Set<JavaExpression> getResetMustCallExpressions(
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      @Nullable TreePath currentPath) {
    AnnotationMirror resetMustCall =
        atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), ResetMustCall.class);
    if (resetMustCall == null) {
      AnnotationMirror resetMustCallList =
          atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), ResetMustCall.List.class);
      if (resetMustCallList == null) {
        return Collections.emptySet();
      }
      // Handle a set of reset must call annotations.
      List<AnnotationMirror> resetMustCalls =
          AnnotationUtils.getElementValueArray(
              resetMustCallList, "value", AnnotationMirror.class, false);
      Set<JavaExpression> results = new HashSet<>();
      if (currentPath == null) {
        currentPath = atypeFactory.getPath(n.getTree());
      }
      for (AnnotationMirror rmc : resetMustCalls) {
        JavaExpression expr = getResetMustCallExpressionsImpl(rmc, n, atypeFactory, currentPath);
        if (expr != null) {
          results.add(expr);
        }
      }
      return results;
    }
    // Handle a single reset must call annotation.
    if (currentPath == null) {
      currentPath = atypeFactory.getPath(n.getTree());
    }
    JavaExpression expr =
        getResetMustCallExpressionsImpl(resetMustCall, n, atypeFactory, currentPath);
    return expr != null ? Collections.singleton(expr) : Collections.emptySet();
  }

  /**
   * Implementation of parsing a single ResetMustCall annotation. See {@link
   * #getResetMustCallExpressions(MethodInvocationNode, GenericAnnotatedTypeFactory)}.
   *
   * @param resetMustCall a reset must call annotation
   * @param n the method invocation of a reset method
   * @param atypeFactory the type factory
   * @return the java expression representing the target, or null if the target is unparseable
   */
  private static @Nullable JavaExpression getResetMustCallExpressionsImpl(
      AnnotationMirror resetMustCall,
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      TreePath currentPath) {
    String targetStrWithoutAdaptation =
        AnnotationUtils.getElementValue(resetMustCall, "value", String.class, true);
    JavaExpressionContext context =
        JavaExpressionParseUtil.JavaExpressionContext.buildContextForMethodUse(
            n, atypeFactory.getChecker());
    // Note that it *is* necessary to parse this string twice - the first time to standardize
    // and viewpoint adapt it via the utility method called on the next line, and the second
    // time (in the try block below) to actually get the relevant expression.
    String targetStr =
        MustCallTransfer.standardizeAndViewpointAdapt(
            targetStrWithoutAdaptation, currentPath, context);
    JavaExpression targetExpr;
    try {
      targetExpr = atypeFactory.parseJavaExpressionString(targetStr, currentPath);
    } catch (JavaExpressionParseException e) {
      atypeFactory
          .getChecker()
          .reportError(
              n.getTree(),
              "mustcall.not.parseable",
              n.getTarget().getMethod().getSimpleName(),
              targetStr);
      return null;
    }
    return targetExpr;
  }

  public static void newTempVar(LocalVariableNode tempVar, Node node) {
    tempVars.put(node, tempVar);
  }

  /*
   * Helper function to standardize and viewpoint adapt a String given a path and a context.
   * Wraps JavaExpressionParseUtil#parse. If a parse exception is encountered, this returns
   * its argument.
   */
  public static String standardizeAndViewpointAdapt(
      String s, TreePath currentPath, JavaExpressionContext context) {
    try {
      return JavaExpressionParseUtil.parse(s, context, currentPath, false).toString();
    } catch (JavaExpressionParseException e) {
      return s;
    }
  }

  @Override
  public TransferResult<CFValue, CFStore> visitStringConcatenate(
      StringConcatenateNode n, TransferInput<CFValue, CFStore> input) {
    return handleStringConcatenation(super.visitStringConcatenate(n, input));
  }

  @Override
  public TransferResult<CFValue, CFStore> visitStringConcatenateAssignment(
      StringConcatenateAssignmentNode n, TransferInput<CFValue, CFStore> in) {
    return handleStringConcatenation(super.visitStringConcatenateAssignment(n, in));
  }

  private TransferResult<CFValue, CFStore> handleStringConcatenation(
      TransferResult<CFValue, CFStore> result) {
    TypeMirror underlyingType = result.getResultValue().getUnderlyingType();
    CFValue newValue = analysis.createSingleAnnotationValue(atypeFactory.BOTTOM, underlyingType);
    return new RegularTransferResult<>(newValue, result.getRegularStore());
  }
}
