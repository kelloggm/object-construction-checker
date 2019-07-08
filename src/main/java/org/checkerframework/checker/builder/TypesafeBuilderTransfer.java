package org.checkerframework.checker.builder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The transfer function for the typesafe builder type system. Its primary job is to refine the
 * types of objects when methods are called on them, so that e.g. after this method sequence:
 *
 * <p>obj.a(); obj.b();
 *
 * <p>the type of obj is @CalledMethods({"a","b"}) (assuming obj had no type beforehand).
 */
public class TypesafeBuilderTransfer extends CFTransfer {
  private final TypesafeBuilderAnnotatedTypeFactory atypefactory;

  public TypesafeBuilderTransfer(final CFAnalysis analysis) {
    super(analysis);
    this.atypefactory = (TypesafeBuilderAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);
    Node receiver = node.getTarget().getReceiver();

    // in the event that the method we're visiting is static
    if (receiver == null) {
      return result;
    }

    AnnotatedTypeMirror currentType = atypefactory.getReceiverType(node.getTree());
    AnnotationMirror type;
    if (currentType == null || !currentType.isAnnotatedInHierarchy(atypefactory.TOP)) {
      type = atypefactory.TOP;
    } else {
      type = currentType.getAnnotationInHierarchy(atypefactory.TOP);
    }

    if (AnnotationUtils.areSame(type, atypefactory.BOTTOM)) {
      return result;
    }

    String methodName = node.getTarget().getMethod().getSimpleName().toString();
    List<String> currentMethods =
        TypesafeBuilderAnnotatedTypeFactory.getValueOfAnnotationWithStringArgument(type);
    List<String> newList =
        Stream.concat(Stream.of(methodName), currentMethods.stream()).collect(Collectors.toList());

    AnnotationMirror newType = atypefactory.createCalledMethods(newList.toArray(new String[0]));

    Receiver receiverReceiver = FlowExpressions.internalReprOf(atypefactory, receiver);

    // For some reason, visitMethodInvocation returns a conditional store. I think this is to
    // support conditional post-condition annotations, based on the comments in CFAbstractTransfer.
    CFStore thenStore = result.getThenStore();
    CFStore elseStore = result.getElseStore();

    thenStore.insertValue(receiverReceiver, newType);
    elseStore.insertValue(receiverReceiver, newType);

    return result;
  }
}
