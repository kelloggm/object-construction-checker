package org.checkerframework.checker.unconnectedsocket;

import org.checkerframework.checker.unconnectedsocket.qual.CannotConnect;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TreeUtils;

public class UnconnectedSocketTransfer extends CFTransfer {

  private final UnconnectedSocketAnnotatedTypeFactory factory;

  public UnconnectedSocketTransfer(CFAnalysis analysis) {
    super(analysis);
    factory = (UnconnectedSocketAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitAssignment(
      AssignmentNode n, TransferInput<CFValue, CFStore> in) {
    if (n.getTarget() instanceof FieldAccessNode) {
      goToTop(n.getExpression(), in);
    }
    return super.visitAssignment(n, in);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    if (factory.getDeclAnnotation(TreeUtils.elementFromUse(n.getTree()), CannotConnect.class)
        != null) {
      return super.visitMethodInvocation(n, in);
    }
    Node receiverNode = n.getTarget().getReceiver();
    if (receiverNode != null) {
      goToTop(receiverNode, in);
    }
    for (Node arg : n.getArguments()) {
      goToTop(arg, in);
    }
    return super.visitMethodInvocation(n, in);
  }

  private void goToTop(Node node, TransferInput<CFValue, CFStore> in) {
    if (in.containsTwoStores()) {
      goToTop(node, in.getThenStore());
      goToTop(node, in.getElseStore());
    } else {
      goToTop(node, in.getRegularStore());
    }
  }

  private void goToTop(Node node, CFStore store) {
    JavaExpression rec = JavaExpression.fromNode(factory, node);
    // If the value isn't cleared, then types are only strengthened.
    // Going to top always weakens the type.
    store.clearValue(rec);
    store.insertValue(rec, factory.TOP);
  }
}
