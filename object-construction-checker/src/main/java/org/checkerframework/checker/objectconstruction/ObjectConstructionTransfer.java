package org.checkerframework.checker.objectconstruction;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.objectconstruction.qual.CalledMethodsPredicate;
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

    // Don't attempt to strengthen @CalledMethodsPredicate annotations, because that would
    // require reasoning about the predicate itself. Instead, start over from top.
    if (AnnotationUtils.areSameByClass(type, CalledMethodsPredicate.class)) {
      type = atypefactory.TOP;
    }

    if (AnnotationUtils.areSame(type, atypefactory.BOTTOM)) {
      return result;
    }

    String methodName = node.getTarget().getMethod().getSimpleName().toString();
    methodName = atypefactory.adjustMethodNameUsingValueChecker(methodName, node.getTree());

    List<String> currentMethods =
        ObjectConstructionAnnotatedTypeFactory.getValueOfAnnotationWithStringArgument(type);
    List<String> newList =
        Stream.concat(Stream.of(methodName), currentMethods.stream()).collect(Collectors.toList());

    AnnotationMirror newType = atypefactory.createCalledMethods(newList.toArray(new String[0]));

    // For some reason, visitMethodInvocation returns a conditional store. I think this is to
    // support conditional post-condition annotations, based on the comments in CFAbstractTransfer.
    CFStore thenStore = result.getThenStore();
    CFStore elseStore = result.getElseStore();

    while (receiver != null) {
      // Insert the new type computed previously as the type of the receiver.
      Receiver receiverReceiver = FlowExpressions.internalReprOf(atypefactory, receiver);
      thenStore.insertValue(receiverReceiver, newType);
      elseStore.insertValue(receiverReceiver, newType);

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

    return result;
  }
}
