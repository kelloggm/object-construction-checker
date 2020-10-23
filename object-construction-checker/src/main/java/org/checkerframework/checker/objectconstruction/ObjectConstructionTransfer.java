package org.checkerframework.checker.objectconstruction;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.objectconstruction.qual.CalledMethodsPredicate;
import org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
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
public class ObjectConstructionTransfer extends CFTransfer {
  private final ObjectConstructionAnnotatedTypeFactory atypefactory;

  public ObjectConstructionTransfer(final CFAnalysis analysis) {
    super(analysis);
    this.atypefactory = (ObjectConstructionAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);

    handleEnsuresCalledMethodVarArgs(node, result);

    Node receiver = node.getTarget().getReceiver();

    // in the event that the method we're visiting is static
    if (receiver == null) {
      return result;
    }

    AnnotatedTypeMirror currentType = atypefactory.getReceiverType(node.getTree());
    String methodName = node.getTarget().getMethod().getSimpleName().toString();
    methodName = atypefactory.adjustMethodNameUsingValueChecker(methodName, node.getTree());
    AnnotationMirror newType = getUpdatedCalledMethodsType(currentType, methodName);
    if (newType == null) {
      return result;
    }

    // For some reason, visitMethodInvocation returns a conditional store. I think this is to
    // support conditional post-condition annotations, based on the comments in CFAbstractTransfer.
    CFStore thenStore = result.getThenStore();
    CFStore elseStore = result.getElseStore();
    Map<TypeMirror, CFStore> exceptionalStores = makeExceptionalStores(node, input);

    while (receiver != null) {
      // Insert the new type computed previously as the type of the receiver.
      Receiver receiverReceiver = FlowExpressions.internalReprOf(atypefactory, receiver);
      thenStore.insertValue(receiverReceiver, newType);
      elseStore.insertValue(receiverReceiver, newType);
      exceptionalStores.values().stream().forEach(s -> s.insertValue(receiverReceiver, newType));

      Tree receiverTree = receiver.getTree();

      // Possibly recurse: if the receiver is itself a method call,
      // then we need to also propagate this new information to its receiver
      // if the method being called has an @This return type.
      //
      // Note that we must check for null, because the tree could be
      // implicit (when calling an instance method on the class itself).
      // In that case, do not attempt to refine either - the receiver is
      // not a method invocation, anyway.
      if (receiverTree == null || receiverTree.getKind() != Tree.Kind.METHOD_INVOCATION) {
        // Do not continue, because the receiver isn't a method invocation itself. The
        // end of the chain of calls has been reached.
        break;
      }

      MethodInvocationTree receiverAsMethodInvocation = (MethodInvocationTree) receiver.getTree();

      if (atypefactory.returnsThis(receiverAsMethodInvocation)) {
        receiver = ((MethodInvocationNode) receiver).getTarget().getReceiver();
      } else {
        // Do not continue, because the method does not return @This.
        break;
      }
    }

    return new ConditionalTransferResult<>(
        result.getResultValue(), thenStore, elseStore, exceptionalStores);
  }

  private AnnotationMirror getUpdatedCalledMethodsType(
      AnnotatedTypeMirror currentType, String... methodNames) {
    AnnotationMirror type;
    if (currentType == null || !currentType.isAnnotatedInHierarchy(atypefactory.TOP)) {
      type = atypefactory.TOP;
    } else {
      type = currentType.getAnnotationInHierarchy(atypefactory.TOP);
    }

    // Don't attempt to strengthen @CalledMethodsPredicate annotations, because that would
    // require reasoning about the predicate itself. Instead, start over from top.
    if (AnnotationUtils.areSameByClass(type, CalledMethodsPredicate.class)) {
      type = atypefactory.TOP;
    }

    if (AnnotationUtils.areSame(type, atypefactory.BOTTOM)) {
      return null;
    }

    List<String> currentMethods =
        ObjectConstructionAnnotatedTypeFactory.getValueOfAnnotationWithStringArgument(type);
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
