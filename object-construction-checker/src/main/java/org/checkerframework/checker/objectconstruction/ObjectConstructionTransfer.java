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
import org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.FlowExpressions;
import org.checkerframework.dataflow.expression.Receiver;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The transfer function for the object construction type system. Its primary job is to refine the
 * types of objects when methods are called on them, so that e.g. after this method sequence:
 *
 * <p>obj.a(); obj.b();
 *
 * <p>the type of obj is @CalledMethods({"a","b"}) (assuming obj had no type beforehand).
 */
public class ObjectConstructionTransfer extends CalledMethodsTransfer {
  private final ObjectConstructionAnnotatedTypeFactory atypefactory;

  /**
   * {@link #makeExceptionalStores(MethodInvocationNode, TransferInput)} requires a TransferInput,
   * but the actual exceptional stores need to be modified in {@link #accumulate(Node,
   * TransferResult, String...)}, which only has access to a TransferResult. So this variable is set
   * to non-null in {@link #visitMethodInvocation(MethodInvocationNode, TransferInput)} before the
   * call to super, which will call accumulate(); this field is then reset to null afterwards to
   * prevent it from being used somewhere it shouldn't be.
   */
  private @Nullable Map<TypeMirror, CFStore> exceptionalStores;

  public ObjectConstructionTransfer(final CFAnalysis analysis) {
    super(analysis);
    this.atypefactory = (ObjectConstructionAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {

    exceptionalStores = makeExceptionalStores(node, input);
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);
    handleEnsuresCalledMethodVarArgs(node, result);
    TransferResult<CFValue, CFStore> finalResult =
        new ConditionalTransferResult<>(
            result.getResultValue(),
            result.getThenStore(),
            result.getElseStore(),
            exceptionalStores);
    exceptionalStores = null;
    return finalResult;
  }

  private AnnotationMirror getUpdatedCalledMethodsType(
      AnnotatedTypeMirror currentType, String... methodNames) {
    AnnotationMirror type;
    if (currentType == null || !currentType.isAnnotatedInHierarchy(atypefactory.top)) {
      type = atypefactory.top;
    } else {
      type = currentType.getAnnotationInHierarchy(atypefactory.top);
    }

    // Don't attempt to strengthen @CalledMethodsPredicate annotations, because that would
    // require reasoning about the predicate itself. Instead, start over from top.
    if (AnnotationUtils.areSameByClass(type, CalledMethodsPredicate.class)) {
      type = atypefactory.top;
    }

    if (AnnotationUtils.areSame(type, atypefactory.bottom)) {
      return null;
    }

    List<String> currentMethods = ValueCheckerUtils.getValueOfAnnotationWithStringArgument(type);
    List<String> newList =
        Stream.concat(Arrays.stream(methodNames), currentMethods.stream())
            .collect(Collectors.toList());

    AnnotationMirror newType = atypefactory.createCalledMethods(newList.toArray(new String[0]));
    return newType;
  }

  private void handleEnsuresCalledMethodVarArgs(
      MethodInvocationNode node, TransferResult<CFValue, CFStore> result) {
    ExecutableElement elt = TreeUtils.elementFromUse(node.getTree());
    AnnotationMirror annot = atypefactory.getDeclAnnotation(elt, EnsuresCalledMethodsVarArgs.class);
    if (annot == null) {
      return;
    }
    String[] ensuredMethodNames =
        AnnotationUtils.getElementValueArray(annot, "value", String.class, true)
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
        AnnotatedTypeMirror currentType = atypefactory.getAnnotatedType(arg.getTree());
        AnnotationMirror newType = getUpdatedCalledMethodsType(currentType, ensuredMethodNames);
        if (newType == null) {
          continue;
        }
        Receiver receiverReceiver = FlowExpressions.internalReprOf(atypefactory, arg);
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
    Receiver target = FlowExpressions.internalReprOf(atypefactory, node);
    if (CFAbstractStore.canInsertReceiver(target)) {
      CFValue flowValue = result.getRegularStore().getValue(target);
      if (flowValue != null) {
        Set<AnnotationMirror> flowAnnos = flowValue.getAnnotations();
        assert flowAnnos.size() <= 1;
        for (AnnotationMirror anno : flowAnnos) {
          if (atypefactory.isAccumulatorAnnotation(anno)) {
            List<String> oldFlowValues =
                ValueCheckerUtils.getValueOfAnnotationWithStringArgument(anno);
            if (oldFlowValues != null) {
              // valuesAsList cannot have its length changed -- it is backed by an
              // array.  getValueOfAnnotationWithStringArgument returns a new,
              // modifiable list.
              oldFlowValues.addAll(valuesAsList);
              valuesAsList = oldFlowValues;
            }
          }
        }
      }
    }
    AnnotationMirror newAnno = atypefactory.createAccumulatorAnnotation(valuesAsList);
    exceptionalStores.values().stream().forEach(s -> s.insertValue(target, newAnno));
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
