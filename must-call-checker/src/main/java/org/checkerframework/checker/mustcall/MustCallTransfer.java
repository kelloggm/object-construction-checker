package org.checkerframework.checker.mustcall;

import com.sun.source.util.TreePath;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.qual.ResetMustCall;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateAssignmentNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
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

  public MustCallTransfer(CFAnalysis analysis) {
    super(analysis);
    atypeFactory = (MustCallAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);
    AnnotationMirror resetMustCall =
        atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), ResetMustCall.class);
    if (resetMustCall != null) {
      String targetStrWithoutAdaptation =
          AnnotationUtils.getElementValue(resetMustCall, "value", String.class, true);
      TreePath currentPath = this.atypeFactory.getPath(n.getTree());
      JavaExpressionContext context =
          JavaExpressionParseUtil.JavaExpressionContext.buildContextForMethodUse(
              n, atypeFactory.getChecker());
      String targetStr =
          standardizeAndViewpointAdapt(targetStrWithoutAdaptation, currentPath, context);
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
        return result;
      }

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
    return result;
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
