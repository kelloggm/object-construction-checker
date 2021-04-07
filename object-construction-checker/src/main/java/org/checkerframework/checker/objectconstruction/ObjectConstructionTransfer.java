package org.checkerframework.checker.objectconstruction;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsTransfer;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallTransfer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The transfer function for the object construction type system. Its primary job is to refine the
 * types of objects when methods are called on them, so that e.g. after this method sequence:
 *
 * <p>obj.a(); obj.b();
 *
 * <p>the type of obj is @CalledMethods({"a","b"}) (assuming obj had no type beforehand).
 */
public class ObjectConstructionTransfer extends CalledMethodsTransfer {

  /**
   * {@link #makeExceptionalStores(MethodInvocationNode, TransferInput)} requires a TransferInput,
   * but the actual exceptional stores need to be modified in {@link #accumulate(Node,
   * TransferResult, String...)}, which only has access to a TransferResult. So this variable is set
   * to non-null in {@link #visitMethodInvocation(MethodInvocationNode, TransferInput)} before the
   * call to super, which will call accumulate(); this field is then reset to null afterwards to
   * prevent it from being used somewhere it shouldn't be.
   */
  private @Nullable Map<TypeMirror, CFStore> exceptionalStores;

  /**
   * Shadowed because we MUST dispatch to the OCC's version of getTypefactoryOfSubchecker to get the
   * correct MCATF.
   */
  private final ObjectConstructionAnnotatedTypeFactory atypeFactory;

  public ObjectConstructionTransfer(final CFAnalysis analysis) {
    super(analysis);
    this.atypeFactory = (ObjectConstructionAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitTernaryExpression(
      TernaryExpressionNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> result = super.visitTernaryExpression(node, input);
    updateStoreWithTempVar(result, node);
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {

    exceptionalStores = makeExceptionalStores(node, input);
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);
    handleEnsuresCalledMethodVarArgs(node, result);
    handleCreatesObligation(node, result);
    TransferResult<CFValue, CFStore> finalResult =
        new ConditionalTransferResult<>(
            result.getResultValue(),
            result.getThenStore(),
            result.getElseStore(),
            exceptionalStores);
    exceptionalStores = null;

    updateStoreWithTempVar(result, node);

    Node receiver = node.getTarget().getReceiver();
    Node accumulationTarget;
    if (this.atypeFactory.getChecker().hasOption(ObjectConstructionChecker.CHECK_MUST_CALL)) {
      // If there is a temporary variable for the receiver, update its type.
      MustCallAnnotatedTypeFactory mcAtf =
          atypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
      accumulationTarget = mcAtf.getTempVar(receiver);
    } else {
      // only update the receiver type
      accumulationTarget = receiver;
    }

    if (accumulationTarget != null) {
      String methodName = node.getTarget().getMethod().getSimpleName().toString();
      methodName = atypeFactory.adjustMethodNameUsingValueChecker(methodName, node.getTree());
      accumulate(accumulationTarget, result, methodName);
    }

    return finalResult;
  }

  void handleCreatesObligation(MethodInvocationNode n, TransferResult<CFValue, CFStore> result) {
    if (!atypeFactory.useAccumulationFrames()) {
      return;
    }

    Set<JavaExpression> targetExprs =
        MustCallTransfer.getCreatesObligationExpressions(n, atypeFactory);
    for (JavaExpression targetExpr : targetExprs) {
      AnnotationMirror defaultType = atypeFactory.top;
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

  @Override
  public TransferResult<CFValue, CFStore> visitObjectCreation(
      ObjectCreationNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> result = super.visitObjectCreation(node, input);
    updateStoreWithTempVar(result, node);
    return result;
  }

  /**
   * This method either creates or looks up the temp var t for node, and then updates the store to
   * give t the same type as node
   *
   * @param node the node to be assigned to a temporal variable
   * @param result the transfer result containing the store to be modified
   */
  public void updateStoreWithTempVar(TransferResult<CFValue, CFStore> result, Node node) {
    if (!this.atypeFactory.getChecker().hasOption(ObjectConstructionChecker.CHECK_MUST_CALL)) {
      return;
    }

    // Must-call obligations on primitives are not supported.
    if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
      MustCallAnnotatedTypeFactory mcAtf =
          atypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
      LocalVariableNode temp = mcAtf.getTempVar(node);
      if (temp != null) {
        atypeFactory.tempVarToNode.put(temp, node.getTree());
        JavaExpression localExp = JavaExpression.fromNode(temp);
        AnnotationMirror anm =
            atypeFactory
                .getAnnotatedType(node.getTree())
                .getAnnotationInHierarchy(atypeFactory.top);
        insertIntoStores(result, localExp, anm == null ? atypeFactory.top : anm);
      }
    }
  }

  private AnnotationMirror getUpdatedCalledMethodsType(
      AnnotatedTypeMirror currentType, String... methodNames) {
    AnnotationMirror type;
    if (currentType == null || !currentType.isAnnotatedInHierarchy(atypeFactory.top)) {
      type = atypeFactory.top;
    } else {
      type = currentType.getAnnotationInHierarchy(atypeFactory.top);
    }

    // Don't attempt to strengthen @CalledMethodsPredicate annotations, because that would
    // require reasoning about the predicate itself. Instead, start over from top.
    if (AnnotationUtils.areSameByName(
        type, "org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate")) {
      type = atypeFactory.top;
    }

    if (AnnotationUtils.areSame(type, atypeFactory.bottom)) {
      return null;
    }

    List<String> currentMethods =
        AnnotationUtils.getElementValueArray(
            type, atypeFactory.calledMethodsValueElement, String.class);
    List<String> newList =
        Stream.concat(Arrays.stream(methodNames), currentMethods.stream())
            .collect(Collectors.toList());

    AnnotationMirror newType = atypeFactory.createCalledMethods(newList.toArray(new String[0]));
    return newType;
  }

  private void handleEnsuresCalledMethodVarArgs(
      MethodInvocationNode node, TransferResult<CFValue, CFStore> result) {
    ExecutableElement elt = TreeUtils.elementFromUse(node.getTree());
    AnnotationMirror annot = atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethodsVarArgs.class);
    if (annot == null) {
      return;
    }
    String[] ensuredMethodNames =
        AnnotationUtils.getElementValueArray(
                annot, atypeFactory.ensuresCalledMethodsVarArgsValueElement, String.class)
            .toArray(new String[0]);
    List<? extends VariableElement> parameters = elt.getParameters();
    int varArgsPos = parameters.size() - 1;
    Node varArgActual = node.getArguments().get(varArgsPos);
    // In the CFG, explicit passing of multiple arguments in the varargs position is represented via
    // an ArrayCreationNode.  This is the only case we handle for now.
    if (varArgActual instanceof ArrayCreationNode) {
      ArrayCreationNode arrayCreationNode = (ArrayCreationNode) varArgActual;
      // add in the called method to all the vararg arguments
      CFStore thenStore = result.getThenStore();
      CFStore elseStore = result.getElseStore();
      for (Node arg : arrayCreationNode.getInitializers()) {
        AnnotatedTypeMirror currentType = atypeFactory.getAnnotatedType(arg.getTree());
        AnnotationMirror newType = getUpdatedCalledMethodsType(currentType, ensuredMethodNames);
        if (newType == null) {
          continue;
        }

        JavaExpression receiverReceiver = JavaExpression.fromNode(arg);
        thenStore.insertValue(receiverReceiver, newType);
        elseStore.insertValue(receiverReceiver, newType);
      }
    }
  }

  @Override
  public void accumulate(Node node, TransferResult<CFValue, CFStore> result, String... values) {
    super.accumulate(node, result, values);
    if (exceptionalStores == null) {
      return;
    }

    List<String> valuesAsList = Arrays.asList(values);
    // If dataflow has already recorded information about the target, fetch it and integrate
    // it into the list of values in the new annotation.
    JavaExpression target = JavaExpression.fromNode(node);
    if (CFAbstractStore.canInsertJavaExpression(target)) {
      CFValue flowValue = result.getRegularStore().getValue(target);
      if (flowValue != null) {
        Set<AnnotationMirror> flowAnnos = flowValue.getAnnotations();
        assert flowAnnos.size() <= 1;
        for (AnnotationMirror anno : flowAnnos) {
          if (atypeFactory.isAccumulatorAnnotation(anno)) {
            List<String> oldFlowValues =
                AnnotationUtils.getElementValueArray(
                    anno, atypeFactory.calledMethodsValueElement, String.class);
            // valuesAsList cannot have its length changed -- it is backed by an
            // array.  getElementValueArray returns a new, modifiable list.
            oldFlowValues.addAll(valuesAsList);
            valuesAsList = oldFlowValues;
          }
        }
      }
      AnnotationMirror newAnno = atypeFactory.createAccumulatorAnnotation(valuesAsList);
      exceptionalStores.values().stream().forEach(s -> s.insertValue(target, newAnno));
    }
  }

  private void insertIntoStores(
      TransferResult<CFValue, CFStore> result, JavaExpression target, AnnotationMirror newAnno) {
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

  private Map<TypeMirror, CFStore> makeExceptionalStores(
      MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
    if (!(node.getBlock() instanceof ExceptionBlock)) {
      // this can happen in some weird (buggy) cases
      return Collections.emptyMap();
    }
    ExceptionBlock block = (ExceptionBlock) node.getBlock();
    Map<TypeMirror, CFStore> result = new LinkedHashMap<>();
    block.getExceptionalSuccessors().keySet().stream()
        .forEach(tm -> result.put(tm, input.getRegularStore().copy()));
    return result;
  }
}
