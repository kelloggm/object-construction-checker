package org.checkerframework.checker.objectconstruction;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallTransfer;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallChoice;
import org.checkerframework.checker.mustcall.qual.ResetMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.objectconstruction.qual.NotOwning;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.com.google.common.base.Predicates;
import org.checkerframework.com.google.common.collect.FluentIterable;
import org.checkerframework.com.google.common.collect.ImmutableSet;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlockImpl;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.cfg.node.ThisNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionContext;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Checks that all methods in {@link org.checkerframework.checker.mustcall.qual.MustCall} object
 * types are invoked before the corresponding objects become unreachable
 */
/* package-private */
class MustCallInvokedChecker {

  /** By default, should we transfer ownership to the caller when a variable is returned? */
  static final boolean TRANSFER_OWNERSHIP_AT_RETURN = true;

  /** {@code @MustCall} errors reported thus far, to avoid duplicates */
  private final Set<LocalVarWithTree> reportedMustCallErrors = new HashSet<>();

  private final Set<Tree> mustCallObligations = new HashSet<>();

  private final ObjectConstructionAnnotatedTypeFactory typeFactory;

  private final ObjectConstructionChecker checker;

  private final CFAnalysis analysis;

  /* package-private */
  MustCallInvokedChecker(
      ObjectConstructionAnnotatedTypeFactory typeFactory,
      ObjectConstructionChecker checker,
      CFAnalysis analysis) {
    this.typeFactory = typeFactory;
    this.checker = checker;
    this.analysis = analysis;
  }

  /**
   * This function traverses the given method CFG and reports an error if "f" isn't called on any
   * local variable node whose class type has @MustCall(f) annotation before the variable goes out
   * of scope. The traverse is a standard worklist algorithm. Worklist and visited entries are
   * BlockWithLocals objects that contain a set of (LocalVariableNode, Tree) pairs for each block. A
   * pair (n, T) represents a local variable node "n" and the latest AssignmentTree "T" that assigns
   * a value to "n".
   *
   * @param cfg the control flow graph of a method
   */
  /* package-private */
  void checkMustCallInvoked(ControlFlowGraph cfg) {
    // add any owning parameters to initial set of variables to track
    BlockWithLocals firstBlockLocals =
        new BlockWithLocals(cfg.getEntryBlock(), computeOwningParameters(cfg));

    Set<BlockWithLocals> visited = new LinkedHashSet<>();
    Deque<BlockWithLocals> worklist = new ArrayDeque<>();

    worklist.add(firstBlockLocals);
    visited.add(firstBlockLocals);

    while (!worklist.isEmpty()) {

      BlockWithLocals curBlockLocals = worklist.removeLast();
      List<Node> nodes = curBlockLocals.block.getNodes();
      // defs to be tracked in successor blocks, updated by code below
      Set<ImmutableSet<LocalVarWithTree>> newDefs =
          new LinkedHashSet<>(curBlockLocals.localSetInfo);

      for (Node node : nodes) {
        if (node instanceof AssignmentNode) {
          handleAssignment((AssignmentNode) node, newDefs);
        } else if (node instanceof ReturnNode) {
          handleReturn((ReturnNode) node, cfg, newDefs);
        } else if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {
          handleInvocation(newDefs, node);
        } else if (node instanceof TernaryExpressionNode) {
          handleTernary(node, newDefs);
        }
      }

      handleSuccessorBlocks(visited, worklist, newDefs, curBlockLocals.block);
    }
  }

  private void handleTernary(Node node, Set<ImmutableSet<LocalVarWithTree>> defs) {
    LocalVariableNode ternaryLocal = typeFactory.getTempVarForTree(node);
    // This can happen when one side of the ternary is null, I think.
    // Without this, we get a crash in tests/mustcall/ZookeeperTernaryCrash.java
    // on the only ternary in that program snippet.
    if (ternaryLocal == null) {
      return;
    }
    // First check then operand
    Node operand = removeCasts(((TernaryExpressionNode) node).getThenOperand());
    LocalVariableNode operandLocal = typeFactory.getTempVarForTree(operand);
    // If operandLocal is null (it happens when then operand is a field or a local variable node),
    // or not tracked by defs (it means we are in else branch), we check else branch
    if (operandLocal == null || !isVarInDefs(defs, operandLocal)) {
      operand = removeCasts(((TernaryExpressionNode) node).getElseOperand());
      operandLocal = typeFactory.getTempVarForTree(operand);
    }
    if (operandLocal != null && isVarInDefs(defs, operandLocal)) {
      LocalVarWithTree latestAssignmentPair = getAssignmentTreeOfVar(defs, operandLocal);
      ImmutableSet<LocalVarWithTree> setContainingOperandLocal =
          getSetContainingAssignmentTreeOfVar(defs, operandLocal);
      ImmutableSet<LocalVarWithTree> newSetContainingOperandLocal =
          FluentIterable.from(setContainingOperandLocal)
              .filter(Predicates.not(Predicates.equalTo(latestAssignmentPair)))
              .append(new LocalVarWithTree(new LocalVariable(ternaryLocal), node.getTree()))
              .toSet();

      defs.remove(setContainingOperandLocal);
      defs.add(newSetContainingOperandLocal);
    }
  }

  private void handleInvocation(Set<ImmutableSet<LocalVarWithTree>> defs, Node node) {
    doOwnershipTransferToParameters(defs, node);
    // Count calls to @ResetMustCall methods as creating new resources, for now.
    if (node instanceof MethodInvocationNode
        && typeFactory.useAccumulationFrames()
        && typeFactory.hasResetMustCall((MethodInvocationNode) node)) {
      checkResetMustCallInvocation(defs, (MethodInvocationNode) node);
      incrementNumMustCall(node.getTree());
    }

    if (shouldSkipInvokeCheck(defs, node)) {
      return;
    }

    if (typeFactory.hasMustCall(node.getTree())) {
      incrementNumMustCall(node.getTree());
    }
    updateDefsWithTempVar(defs, node);
  }

  /**
   * If node is an invocation of a this or super constructor that has a MCC return type and an MCC
   * parameter, check if any variable in defs is an MCC parameter being passed to the other
   * constructor. If so, remove it from defs.
   *
   * @param defs current defs
   * @param node a method or constructor invocation
   */
  private void handleThisOrSuperConstructorMustCallChoice(
      Set<ImmutableSet<LocalVarWithTree>> defs, Node node) {
    if (node instanceof ObjectCreationNode || node instanceof MethodInvocationNode) {
      Node mccParam = getVarOrTempVarPassedAsMustCallChoiceParam(node);
      // if the MCC param is also a MCC def in the def set, then remove it -
      // its obligation has been fulfilled by being passed on to another MCC method/constructor
      if (mccParam instanceof LocalVariableNode
          && isVarInDefs(defs, (LocalVariableNode) mccParam)) {
        LocalVarWithTree lvt = getAssignmentTreeOfVar(defs, (LocalVariableNode) mccParam);
        if (lvt.isMustCallChoice) {
          ImmutableSet<LocalVarWithTree> setContainingMustCallChoiceParamLocal =
              getSetContainingAssignmentTreeOfVar(defs, (LocalVariableNode) mccParam);
          defs.remove(setContainingMustCallChoiceParamLocal);
        }
      }
    }
  }

  /**
   * Checks that an invocation of a ResetMustCall method is valid. Such an invocation is valid if
   * one of the following conditions is true: 1) the target is an owning pointer 2) the target is
   * tracked in newdefs 3) the method in which the invocation occurs also has an @ResetMustCall
   * annotation, with the same target
   *
   * <p>If none of the above are true, this method issues a reset.not.owning error.
   *
   * @param newDefs the local variables that have been defined in the current compilation unit (and
   *     are therefore going to be checked later)
   * @param node a method invocation node, invoking a method with a ResetMustCall annotation
   */
  private void checkResetMustCallInvocation(
      Set<ImmutableSet<LocalVarWithTree>> newDefs, MethodInvocationNode node) {

    TreePath currentPath = typeFactory.getPath(node.getTree());
    Set<JavaExpression> targetExprs =
        MustCallTransfer.getResetMustCallExpressions(node, typeFactory, currentPath);
    Set<JavaExpression> missing = new HashSet<>();
    for (JavaExpression target : targetExprs) {
      if (target instanceof LocalVariable) {

        Element elt = ((LocalVariable) target).getElement();
        if (typeFactory.getDeclAnnotation(elt, Owning.class) != null) {
          // if the target is an Owning param, this satisfies case 1
          return;
        }

        for (ImmutableSet<LocalVarWithTree> defAliasSet : newDefs) {
          for (LocalVarWithTree localVarWithTree : defAliasSet) {
            if (target.equals(localVarWithTree.localVar)) {
              // satisfies case 2 above
              return;
            }
          }
        }
      }
      if (target instanceof FieldAccess) {
        Element elt = ((FieldAccess) target).getField();
        if (typeFactory.getDeclAnnotation(elt, Owning.class) != null) {
          // if the target is an Owning field, this satisfies case 1
          return;
        }
      }

      MethodTree enclosingMethod = TreePathUtil.enclosingMethod(currentPath);
      if (enclosingMethod != null) {
        ExecutableElement enclosingElt = TreeUtils.elementFromDeclaration(enclosingMethod);
        AnnotationMirror enclosingResetMustCall =
            typeFactory.getDeclAnnotation(enclosingElt, ResetMustCall.class);
        if (enclosingResetMustCall != null) {
          String enclosingTargetStrWithoutAdaptation =
              AnnotationUtils.getElementValue(enclosingResetMustCall, "value", String.class, true);
          JavaExpressionContext enclosingContext =
              JavaExpressionParseUtil.JavaExpressionContext.buildContextForMethodDeclaration(
                  enclosingMethod, currentPath, checker);
          String enclosingTargetStr =
              MustCallTransfer.standardizeAndViewpointAdapt(
                  enclosingTargetStrWithoutAdaptation, currentPath, enclosingContext);
          if (enclosingTargetStr.equals(target.toString())) {
            // The enclosing method also has a corresponding ResetMustCall annotation, so this
            // satisfies case 3.
            return;
          }
        }
      }
      missing.add(target);
    }
    String missingStrs =
        missing.stream().map(JavaExpression::toString).collect(Collectors.joining(", "));
    checker.reportError(node.getTree(), "reset.not.owning", missingStrs);
  }

  /**
   * Given a node representing a method or constructor call, checks that if the call has a non-empty
   * {@code @MustCall} type, then its result is pseudo-assigned to some location that can take
   * ownership of the result. Searches for the set of same resources in defs and add the new
   * LocalVarWithTree to it if one exists. Otherwise creates a new set.
   */
  private void updateDefsWithTempVar(Set<ImmutableSet<LocalVarWithTree>> defs, Node node) {
    Tree tree = node.getTree();
    LocalVariableNode temporaryLocal = typeFactory.getTempVarForTree(node);
    if (temporaryLocal != null) {

      LocalVarWithTree lhsLocalVarWithTreeNew =
          new LocalVarWithTree(new LocalVariable(temporaryLocal), tree);

      Node sameResource = null;
      // Set sameResource to the MCC parameter if any exists, otherwise it remains null
      if (node instanceof ObjectCreationNode || node instanceof MethodInvocationNode) {
        sameResource = getVarOrTempVarPassedAsMustCallChoiceParam(node);
      }

      // If sameResource is still null and node returns @This, set sameResource to the receiver
      if (sameResource == null
          && node instanceof MethodInvocationNode
          && (typeFactory.returnsThis((MethodInvocationTree) tree))) {
        sameResource = ((MethodInvocationNode) node).getTarget().getReceiver();
        if (sameResource instanceof MethodInvocationNode) {
          sameResource = typeFactory.getTempVarForTree(sameResource);
        }
      }

      if (sameResource != null) {
        sameResource = removeCasts(sameResource);
      }

      // If sameResource is local variable tracked by defs, add lhsLocalVarWithTreeNew to the set
      // containing sameResource. Otherwise, add it to a new set
      if (sameResource instanceof LocalVariableNode
          && isVarInDefs(defs, (LocalVariableNode) sameResource)) {
        ImmutableSet<LocalVarWithTree> setContainingMustCallChoiceParamLocal =
            getSetContainingAssignmentTreeOfVar(defs, (LocalVariableNode) sameResource);
        ImmutableSet<LocalVarWithTree> newSetContainingMustCallChoiceParamLocal =
            FluentIterable.from(setContainingMustCallChoiceParamLocal)
                .append(lhsLocalVarWithTreeNew)
                .toSet();
        defs.remove(setContainingMustCallChoiceParamLocal);
        defs.add(newSetContainingMustCallChoiceParamLocal);
      } else if (!(sameResource instanceof LocalVariableNode)) {
        defs.add(ImmutableSet.of(lhsLocalVarWithTreeNew));
      }
    }
  }

  /**
   * Checks for cases where we do not need to track a method. We can skip the check when the method
   * invocation is a call to "this" or a super constructor call, when the method's return type is
   * annotated with MustCallChoice and the argument in the corresponding position is an owning
   * field, or when the method's return type is non-owning, which can either be because the method
   * has no return type or because it is annotated with {@link NotOwning}.
   */
  private boolean shouldSkipInvokeCheck(Set<ImmutableSet<LocalVarWithTree>> defs, Node node) {
    Tree callTree = node.getTree();
    if (callTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree methodInvokeTree = (MethodInvocationTree) callTree;

      if (TreeUtils.isSuperConstructorCall(methodInvokeTree)
          || TreeUtils.isThisConstructorCall(methodInvokeTree)) {
        handleThisOrSuperConstructorMustCallChoice(defs, node);
        return true;
      }
      return returnTypeIsMustCallChoiceWithIgnorable((MethodInvocationNode) node)
          || hasNotOwningReturnType((MethodInvocationNode) node);
    }
    return false;
  }

  /**
   * Returns true if this node represents a method invocation of a must-call choice method, where
   * the other must call choice is some ignorable pointer, such as an owning field or a pointer that
   * is guaranteed to be non-owning, such as this or a non-owning field.
   *
   * @param node a method invocation node
   * @return if this is the invocation of a method whose return type is MCC with an owning field or
   *     a non-owning pointer
   */
  private boolean returnTypeIsMustCallChoiceWithIgnorable(MethodInvocationNode node) {
    Node mccParam = getVarOrTempVarPassedAsMustCallChoiceParam(node);
    return mccParam instanceof FieldAccessNode || mccParam instanceof ThisNode;
  }

  /**
   * Checks if {@code node} is nested inside a {@link TypeCastNode} or a {@link
   * TernaryExpressionNode}, by looking at the successor block in the CFG.
   *
   * @param node the CFG node
   * @return {@code true} if {@code node} is in a {@link SingleSuccessorBlock} {@code b}, the first
   *     {@link Node} in {@code b}'s successor block is a {@link TypeCastNode} or a {@link
   *     TernaryExpressionNode}, and {@code node} is an operand of the successor node; {@code false}
   *     otherwise
   */
  private boolean nestedInCastOrTernary(Node node) {
    if (!(node.getBlock() instanceof SingleSuccessorBlock)) {
      return false;
    }
    Block successorBlock = ((SingleSuccessorBlock) node.getBlock()).getSuccessor();
    if (successorBlock != null) {
      List<Node> succNodes = successorBlock.getNodes();
      if (succNodes.size() > 0) {
        Node succNode = succNodes.get(0);
        if (succNode instanceof TypeCastNode) {
          return ((TypeCastNode) succNode).getOperand().equals(node);
        } else if (succNode instanceof TernaryExpressionNode) {
          TernaryExpressionNode ternaryExpressionNode = (TernaryExpressionNode) succNode;
          return ternaryExpressionNode.getThenOperand().equals(node)
              || ternaryExpressionNode.getElseOperand().equals(node);
        }
      }
    }
    return false;
  }

  /**
   * logic to transfer ownership of locals to {@code @Owning} parameters at a method or constructor
   * call
   */
  private void doOwnershipTransferToParameters(
      Set<ImmutableSet<LocalVarWithTree>> newDefs, Node node) {
    List<Node> arguments = getArgumentsOfMethodOrConstructor(node);
    List<? extends VariableElement> formals = getFormalsOfMethodOrConstructor(node);

    if (arguments.size() != formals.size()) {
      // this could happen, e.g., with varargs, or with strange cases like generated Enum
      // constructors
      // for now, just skip this case
      // TODO allow for ownership transfer here if needed in future
      return;
    }
    for (int i = 0; i < arguments.size(); i++) {
      Node n = arguments.get(i);
      LocalVariableNode local = null;
      if (n instanceof LocalVariableNode) {
        local = (LocalVariableNode) n;
      } else if (typeFactory.getTempVarForTree(n) != null) {
        local = typeFactory.getTempVarForTree(n);
      }

      if (local != null && isVarInDefs(newDefs, local)) {

        // check if formal has an @Owning annotation
        VariableElement formal = formals.get(i);
        Set<AnnotationMirror> annotationMirrors = typeFactory.getDeclAnnotations(formal);

        if (annotationMirrors.stream()
            .anyMatch(anno -> AnnotationUtils.areSameByClass(anno, Owning.class))) {
          // transfer ownership!
          newDefs.remove(getSetContainingAssignmentTreeOfVar(newDefs, local));
        }
      }
    }
  }

  private void handleReturn(
      ReturnNode node, ControlFlowGraph cfg, Set<ImmutableSet<LocalVarWithTree>> newDefs) {
    if (isTransferOwnershipAtReturn(cfg)) {
      Node result = node.getResult();
      Node temp = typeFactory.getTempVarForTree(result);
      if (temp != null) {
        result = temp;
      }
      if (result instanceof LocalVariableNode && isVarInDefs(newDefs, (LocalVariableNode) result)) {
        newDefs.remove(getSetContainingAssignmentTreeOfVar(newDefs, (LocalVariableNode) result));
      }
    }
  }

  /**
   * Should we transfer ownership to the return type of the method corresponding to a CFG? Returns
   * true when either (1) there is an explicit {@link Owning} annotation on the return type or (2)
   * the policy is to transfer ownership by default, and there is no {@link NotOwning} annotation on
   * the return type
   */
  private boolean isTransferOwnershipAtReturn(ControlFlowGraph cfg) {
    UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
    if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
      // TODO: lambdas?
      MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
      ExecutableElement executableElement = TreeUtils.elementFromDeclaration(method);
      return (typeFactory.getDeclAnnotation(executableElement, Owning.class) != null)
          || (TRANSFER_OWNERSHIP_AT_RETURN
              && typeFactory.getDeclAnnotation(executableElement, NotOwning.class) == null);
    }
    return false;
  }

  private void handleAssignment(AssignmentNode node, Set<ImmutableSet<LocalVarWithTree>> newDefs) {
    Node rhs = removeCasts(node.getExpression());
    if (typeFactory.getTempVarForTree(rhs) != null) {
      rhs = typeFactory.getTempVarForTree(rhs);
    }
    handleAssignFromRHS(node, newDefs, rhs);
  }

  private Node removeCasts(Node node) {
    while (node instanceof TypeCastNode) {
      node = ((TypeCastNode) node).getOperand();
    }
    return node;
  }

  private void handleAssignFromRHS(
      AssignmentNode node, Set<ImmutableSet<LocalVarWithTree>> newDefs, Node rhs) {
    Node lhs = node.getTarget();
    Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());

    // Ownership transfer to @Owning field
    if (lhsElement.getKind().equals(ElementKind.FIELD)) {
      boolean isOwningField = typeFactory.getDeclAnnotation(lhsElement, Owning.class) != null;
      // Check that there is no obligation on the lhs, if the field is non-final and owning.
      if (isOwningField
          && typeFactory.useAccumulationFrames()
          && !ElementUtils.isFinal(lhsElement)) {
        checkReassignmentToField(node, newDefs);
      }
      // Remove obligations from local variables, now that the owning field is responsible.
      // (When accumulation frames are turned off, non-final fields cannot take ownership).
      if (isOwningField
          && rhs instanceof LocalVariableNode
          && isVarInDefs(newDefs, (LocalVariableNode) rhs)
          && (typeFactory.useAccumulationFrames() || ElementUtils.isFinal(lhsElement))) {
        Set<LocalVarWithTree> setContainingRhs =
            getSetContainingAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs);
        newDefs.remove(setContainingRhs);
      }
    } else if (lhs instanceof LocalVariableNode
        && !isTryWithResourcesVariable((LocalVariableNode) lhs)) {
      // Reassignment to the lhs
      if (isVarInDefs(newDefs, (LocalVariableNode) lhs)) {
        ImmutableSet<LocalVarWithTree> setContainingLhs =
            getSetContainingAssignmentTreeOfVar(newDefs, (LocalVariableNode) lhs);
        LocalVarWithTree latestAssignmentPair =
            getAssignmentTreeOfVar(newDefs, (LocalVariableNode) lhs);
        // If the rhs is not MCC with the lhs, we will remove the latest assignment pair of lhs
        // from the newDefs. If the lhs is the only pointer to the previous resource then we will
        // do MustCall checks for that resource
        if (setContainingLhs.size() > 1) {
          // If the setContainingLatestAssignmentPair has more LocalVarWithTree, remove
          // latestAssignmentPair
          ImmutableSet<LocalVarWithTree> newSetContainingLhs =
              FluentIterable.from(setContainingLhs)
                  .filter(Predicates.not(Predicates.equalTo(latestAssignmentPair)))
                  .toSet();
          newDefs.remove(setContainingLhs);
          newDefs.add(newSetContainingLhs);
        } else {
          // If the setContainingLatestAssignmentPair size is one and the rhs is not MCC with the
          // lhs
          MustCallAnnotatedTypeFactory mcAtf =
              typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);

          checkMustCall(
              setContainingLhs,
              typeFactory.getStoreBefore(node),
              mcAtf.getStoreBefore(node),
              "variable overwritten by assignment " + node.getTree());
          newDefs.remove(setContainingLhs);
        }
      }

      // If the rhs is a temporary variable, we replace it with the lhs
      if (typeFactory.tempVarToNode.containsKey(rhs)) {
        if (isVarInDefs(newDefs, (LocalVariableNode) rhs)) {
          LocalVarWithTree latestAssignmentPair =
              getAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs);
          ImmutableSet<LocalVarWithTree> setContainingRhsTempVar =
              getSetContainingAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs);
          ImmutableSet<LocalVarWithTree> newSetContainingRhsTempVar =
              FluentIterable.from(setContainingRhsTempVar)
                  .filter(Predicates.not(Predicates.equalTo(latestAssignmentPair)))
                  .append(
                      new LocalVarWithTree(
                          new LocalVariable((LocalVariableNode) lhs), node.getTree()))
                  .toSet();

          newDefs.remove(setContainingRhsTempVar);
          newDefs.add(newSetContainingRhsTempVar);
        }
      }
      // Ownership Transfer
      if (rhs instanceof LocalVariableNode && isVarInDefs(newDefs, (LocalVariableNode) rhs)) {
        // If the rhs is a LocalVariableNode that exists in the newDefs (Note that if a
        // localVariableNode exists in the newDefs it means it isn't assigned to a null
        // literals), then it adds the lhs to the set containing rhs
        ImmutableSet<LocalVarWithTree> setContainingRhs =
            getSetContainingAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs);
        LocalVarWithTree lhsLocalVarWithTreeNew =
            new LocalVarWithTree(new LocalVariable((LocalVariableNode) lhs), node.getTree());
        // It is important that newDefs contains the set of these locals - that is, their
        // aliasing relationship - because either one could have a reset method called on it,
        // which would create a new obligation.
        ImmutableSet<LocalVarWithTree> newSetContainingRhsTempVar =
            FluentIterable.from(setContainingRhs).append(lhsLocalVarWithTreeNew).toSet();
        newDefs.add(newSetContainingRhsTempVar);
        newDefs.remove(setContainingRhs);
      }
    } else if (lhs instanceof LocalVariableNode
        && isTryWithResourcesVariable((LocalVariableNode) lhs)
        && rhs instanceof LocalVariableNode) {
      // If the lhs is a resource variable, then we remove the set containing rhs from the newDefs
      Set<LocalVarWithTree> setContainingRhs =
          getSetContainingAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs);
      newDefs.remove(setContainingRhs);
    }
  }

  /**
   * Checks that the given re-assignment to a non-final, owning field is valid. Issues an error if
   * not. A re-assignment is valid if the called methods type of the lhs before the assignment
   * satisfies the must-call obligations.
   *
   * @param node an assignment to a non-final, owning field
   * @param newDefs
   */
  private void checkReassignmentToField(
      AssignmentNode node, Set<ImmutableSet<LocalVarWithTree>> newDefs) {

    Node lhsNode = node.getTarget();

    if (!(lhsNode instanceof FieldAccessNode)) {
      throw new BugInCF(
          "tried to check reassignment to a field for a non-field node: "
              + node
              + " of type: "
              + node.getClass());
    }

    FieldAccessNode lhs = (FieldAccessNode) lhsNode;
    Node receiver = lhs.getReceiver();

    // TODO: it would be better to defer getting the path until after we check
    // for a ResetMustCall annotation, because getting the path can be expensive.
    // It might be possible to exploit the CFG structure to find the containing
    // method (rather than using the path, as below), because if a method is being
    // analyzed then it should be the root of the CFG (I think).
    TreePath currentPath = typeFactory.getPath(node.getTree());
    MethodTree enclosingMethod = TreePathUtil.enclosingMethod(currentPath);

    if (enclosingMethod == null) {
      // Assignments outside of methods must be field initializers, which
      // are always safe.
      return;
    }

    // Check that there is a corresponding resetMustCall annotation, unless this is
    // 1) an assignment to a field of a newly-declared local variable that can't be in scope
    // for the containing method, 2) the rhs is a null literal (so there's nothing to reset).
    if (!(receiver instanceof LocalVariableNode
            && isVarInDefs(newDefs, (LocalVariableNode) receiver))
        && !(node.getExpression() instanceof NullLiteralNode)) {
      checkEnclosingMethodIsResetMC(node, enclosingMethod, currentPath);
    }

    MustCallAnnotatedTypeFactory mcTypeFactory =
        typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotationMirror mcAnno =
        mcTypeFactory.getAnnotationFromJavaExpression(
            JavaExpression.fromNode(mcTypeFactory, lhs), node.getTree(), MustCall.class);
    List<String> mcValues = ValueCheckerUtils.getValueOfAnnotationWithStringArgument(mcAnno);

    if (mcValues.isEmpty()) {
      return;
    }

    CFStore cmStoreBefore = typeFactory.getStoreBefore(node);
    CFValue cmValue = cmStoreBefore == null ? null : cmStoreBefore.getValue(lhs);
    AnnotationMirror cmAnno =
        cmValue == null
            ? typeFactory.top
            : cmValue.getAnnotations().stream()
                .filter(anno -> AnnotationUtils.areSameByClass(anno, CalledMethods.class))
                .findAny()
                .orElse(typeFactory.top);

    if (!calledMethodsSatisfyMustCall(mcValues, cmAnno)) {
      Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());
      if (!checker.shouldSkipUses(lhsElement)) {
        checker.reportError(
            node.getTree(),
            "required.method.not.called",
            formatMissingMustCallMethods(mcValues),
            lhsElement.asType().toString(),
            " Non-final owning field might be overwritten");
      }
    }
  }

  /**
   * Checks that the method that encloses an assignment is marked with @ResetMustCall annotation
   * whose target is the object whose field is being re-assigned.
   *
   * @param node an assignment node whose lhs is a non-final, owning field
   * @param enclosingMethod the MethodTree in which the re-assignment takes place
   * @param currentPath the currentPath
   */
  private void checkEnclosingMethodIsResetMC(
      AssignmentNode node, MethodTree enclosingMethod, TreePath currentPath) {
    Node lhs = node.getTarget();
    if (!(lhs instanceof FieldAccessNode)) {
      return;
    }

    String receiverString = receiverAsString((FieldAccessNode) lhs);
    if (TreeUtils.isConstructor(enclosingMethod)) {
      // Resetting a constructor doesn't make sense.
      return;
    }
    ExecutableElement enclosingElt = TreeUtils.elementFromDeclaration(enclosingMethod);
    AnnotationMirror resetMustCall =
        typeFactory.getDeclAnnotation(enclosingElt, ResetMustCall.class);
    AnnotationMirror resetMustCalls =
        typeFactory.getDeclAnnotation(enclosingElt, ResetMustCall.List.class);
    if (resetMustCall == null && resetMustCalls == null) {
      checker.reportError(
          enclosingMethod,
          "missing.reset.mustcall",
          receiverString,
          ((FieldAccessNode) lhs).getFieldName());
      return;
    }

    Set<String> targetStrsWithoutAdaptation;
    if (resetMustCall != null) {
      targetStrsWithoutAdaptation =
          Collections.singleton(
              AnnotationUtils.getElementValue(resetMustCall, "value", String.class, true));
    } else {
      // multiple reset must calls
      List<AnnotationMirror> resetMustCallAnnos =
          AnnotationUtils.getElementValueArray(
              resetMustCalls, "value", AnnotationMirror.class, false);
      targetStrsWithoutAdaptation = new HashSet<>();
      for (AnnotationMirror rmc : resetMustCallAnnos) {
        targetStrsWithoutAdaptation.add(
            AnnotationUtils.getElementValue(rmc, "value", String.class, true));
      }
    }
    JavaExpressionContext context =
        JavaExpressionParseUtil.JavaExpressionContext.buildContextForMethodDeclaration(
            enclosingMethod, currentPath, checker);
    String checked = "";
    for (String targetStrWithoutAdaptation : targetStrsWithoutAdaptation) {
      String targetStr =
          MustCallTransfer.standardizeAndViewpointAdapt(
              targetStrWithoutAdaptation, currentPath, context);
      if (targetStr.equals(receiverString)) {
        // This reset must call annotation matches.
        return;
      }
      if ("".equals(checked)) {
        checked += targetStr;
      } else {
        checked += ", " + targetStr;
      }
    }
    checker.reportError(
        enclosingMethod,
        "incompatible.reset.mustcall",
        receiverString,
        ((FieldAccessNode) lhs).getFieldName(),
        checked);
  }

  /** Gets a standardized name for an object whose field is being re-assigned. */
  private String receiverAsString(FieldAccessNode lhs) {
    Node receiver = lhs.getReceiver();
    if (receiver instanceof ThisNode) {
      return "this";
    }
    if (receiver instanceof LocalVariableNode) {

      return ((LocalVariableNode) receiver).getName();
    }
    throw new BugInCF(
        "unexpected receiver of field assignment: " + receiver + " of type " + receiver.getClass());
  }

  /**
   * This method tries to find a local variable passed as a @MustCallChoice parameter. In the base
   * case, if {@code node} is a local variable, it just gets returned. Otherwise, if node is a call
   * (or a call wrapped in a cast), the code finds the parameter passed in the @MustCallChoice
   * position, and recurses on that parameter.
   *
   * @param node
   * @return {@code node} iff {@code node} represents a local variable that is passed as
   *     a @MustCallChoice parameter, otherwise null
   */
  private @Nullable Node getVarOrTempVarPassedAsMustCallChoiceParam(Node node) {
    node = removeCasts(node);
    Node n = null;
    if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {

      if (!typeFactory.hasMustCallChoice(node.getTree())) {
        return null;
      }

      List<Node> arguments = getArgumentsOfMethodOrConstructor(node);
      List<? extends VariableElement> formals = getFormalsOfMethodOrConstructor(node);

      for (int i = 0; i < arguments.size(); i++) {
        if (typeFactory.hasMustCallChoice(formals.get(i))) {
          n = arguments.get(i);
          if (n instanceof MethodInvocationNode || n instanceof ObjectCreationNode) {
            n = typeFactory.getTempVarForTree(n);
            break;
          }
        }
      }

      // If node does't have @MustCallChoice parameter then it checks the receiver parameter
      if (n == null && node instanceof MethodInvocationNode) {
        n = ((MethodInvocationNode) node).getTarget().getReceiver();
        if (n instanceof MethodInvocationNode || n instanceof ObjectCreationNode) {
          n = typeFactory.getTempVarForTree(n);
        }
      }
    }

    return n;
  }

  private List<Node> getArgumentsOfMethodOrConstructor(Node node) {
    List<Node> arguments;
    if (node instanceof MethodInvocationNode) {
      MethodInvocationNode invocationNode = (MethodInvocationNode) node;
      arguments = invocationNode.getArguments();
    } else {
      if (!(node instanceof ObjectCreationNode)) {
        throw new BugInCF("unexpected node type " + node.getClass());
      }
      arguments = ((ObjectCreationNode) node).getArguments();
    }
    return arguments;
  }

  private List<? extends VariableElement> getFormalsOfMethodOrConstructor(Node node) {
    ExecutableElement executableElement;
    if (node instanceof MethodInvocationNode) {
      MethodInvocationNode invocationNode = (MethodInvocationNode) node;
      executableElement = TreeUtils.elementFromUse(invocationNode.getTree());
    } else {
      if (!(node instanceof ObjectCreationNode)) {
        throw new BugInCF("unexpected node type " + node.getClass());
      }
      executableElement = TreeUtils.elementFromUse(((ObjectCreationNode) node).getTree());
    }

    List<? extends VariableElement> formals = executableElement.getParameters();
    return formals;
  }

  private boolean hasNotOwningReturnType(MethodInvocationNode node) {
    MethodInvocationTree methodInvocationTree = node.getTree();
    ExecutableElement executableElement = TreeUtils.elementFromUse(methodInvocationTree);
    // void methods are "not owning" by construction
    return (ElementUtils.getType(executableElement).getKind() == TypeKind.VOID)
        || (typeFactory.getDeclAnnotation(executableElement, NotOwning.class) != null);
  }

  /**
   * get all successor blocks for some block, except for those corresponding to ignored exceptions
   *
   * @param block input block
   * @return set of pairs (b, t), where b is a relevant successor block, and t is the type of
   *     exception for the CFG edge from block to b, or {@code null} if b is a non-exceptional
   *     successor
   */
  private Set<Pair<Block, @Nullable TypeMirror>> getRelevantSuccessors(Block block) {
    if (block.getType() == Block.BlockType.EXCEPTION_BLOCK) {
      ExceptionBlock excBlock = (ExceptionBlock) block;
      Set<Pair<Block, @Nullable TypeMirror>> result = new LinkedHashSet<>();
      // regular successor
      Block regularSucc = excBlock.getSuccessor();
      if (regularSucc != null) {
        result.add(Pair.of(regularSucc, null));
      }
      // relevant exception successors
      Map<TypeMirror, Set<Block>> exceptionalSuccessors = excBlock.getExceptionalSuccessors();
      for (Map.Entry<TypeMirror, Set<Block>> entry : exceptionalSuccessors.entrySet()) {
        TypeMirror exceptionType = entry.getKey();
        if (!isIgnoredExceptionType(((Type) exceptionType).tsym.getQualifiedName())) {
          for (Block exSucc : entry.getValue()) {
            result.add(Pair.of(exSucc, exceptionType));
          }
        }
      }
      return result;
    } else {
      return block.getSuccessors().stream()
          .map(b -> Pair.<Block, TypeMirror>of(b, null))
          .collect(Collectors.toSet());
    }
  }

  private void handleSuccessorBlocks(
      Set<BlockWithLocals> visited,
      Deque<BlockWithLocals> worklist,
      Set<ImmutableSet<LocalVarWithTree>> defs,
      Block block) {
    String outOfScopeReason;
    if (block instanceof ExceptionBlock) {
      outOfScopeReason =
          "possible exceptional exit due to " + ((ExceptionBlock) block).getNode().getTree();
    } else {
      // technically the variable may be going out of scope before the method exit, but that
      // doesn't seem to provide additional helpful information
      outOfScopeReason = "regular method exit";
    }
    List<Node> nodes = block.getNodes();
    for (Pair<Block, @Nullable TypeMirror> succAndExcType : getRelevantSuccessors(block)) {
      Set<ImmutableSet<LocalVarWithTree>> defsCopy = new LinkedHashSet<>(defs);
      Set<ImmutableSet<LocalVarWithTree>> toRemove = new LinkedHashSet<>();
      Block succ = succAndExcType.first;
      TypeMirror exceptionType = succAndExcType.second;
      String reasonForSucc =
          exceptionType == null
              ? outOfScopeReason
              : outOfScopeReason + " with exception type " + exceptionType.toString();
      CFStore succRegularStore = analysis.getInput(succ).getRegularStore();
      for (ImmutableSet<LocalVarWithTree> setAssign : defs) {
        // If the successor block is the exit block or if the variable is going out of scope
        boolean noSuccInfo =
            setAssign.stream()
                .allMatch(assign -> succRegularStore.getValue(assign.localVar) == null);
        if (succ instanceof SpecialBlockImpl || noSuccInfo) {
          MustCallAnnotatedTypeFactory mcAtf =
              typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);

          // Remove the temporary variable defined for a node that throws an exception from the
          // exceptional successors
          if (succAndExcType.second != null) {
            Node exceptionalNode = removeCasts(((ExceptionBlock) block).getNode());
            LocalVariableNode localVariable = typeFactory.getTempVarForTree(exceptionalNode);
            if (localVariable != null
                && setAssign.stream()
                    .allMatch(
                        local -> local.localVar.getElement().equals(localVariable.getElement()))) {
              toRemove.add(setAssign);
              break;
            }
          }

          if (nodes.size() == 1 && nestedInCastOrTernary(block.getNodes().get(0))) {
            break;
          }

          if (nodes.size() == 0) { // If the cur block is special or conditional block
            // Use the store from the block actually being analyzed, rather than succRegularStore,
            // if succRegularStore contains no information about the variables of interest.
            // In the case where none of the local variables in setAssign appear in
            // succRegularStore, the variable is going out of scope, and it doesn't make
            // sense to pass succRegularStore to checkMustCall - the successor store will
            // not have any information about it, by construction, and
            // any information in the previous store remains true. If any locals do appear
            // in succRegularStore, we will always use that store.
            CFStore cmStore =
                noSuccInfo ? analysis.getInput(block).getRegularStore() : succRegularStore;
            CFStore mcStore = mcAtf.getStoreForBlock(noSuccInfo, block, succ);
            checkMustCall(setAssign, cmStore, mcStore, reasonForSucc);
          } else { // If the cur block is Exception/Regular block then it checks MustCall
            // annotation in the store right after the last node
            Node last = nodes.get(nodes.size() - 1);
            CFStore cmStoreAfter = typeFactory.getStoreAfter(last);
            // If this is an exceptional block, check the MC store beforehand to avoid
            // issuing an error about a call to a ResetMustCall method that might throw
            // an exception. Otherwise, use the store after.
            CFStore mcStore;
            if (exceptionType != null && isInvocationOfRMCMethod(last)) {
              mcStore = mcAtf.getStoreBefore(last);
            } else {
              mcStore = mcAtf.getStoreAfter(last);
            }
            checkMustCall(setAssign, cmStoreAfter, mcStore, reasonForSucc);
          }

          toRemove.add(setAssign);
        } else {
          // handling the case where some vars go out of scope in the set
          Set<LocalVarWithTree> setAssignCopy = new LinkedHashSet<>(setAssign);
          setAssignCopy.removeIf(assign -> succRegularStore.getValue(assign.localVar) == null);
          defsCopy.remove(setAssign);
          defsCopy.add(ImmutableSet.copyOf(setAssignCopy));
        }
      }

      defsCopy.removeAll(toRemove);
      propagate(new BlockWithLocals(succ, defsCopy), visited, worklist);
    }
  }

  /**
   * returns true if node is a MethodInvocationNode of a method with a ResetMustCall annotation.
   *
   * @param node a node
   * @return true if node is a MethodInvocationNode of a method with a ResetMustCall annotation
   */
  private boolean isInvocationOfRMCMethod(Node node) {
    if (!(node instanceof MethodInvocationNode)) {
      return false;
    }
    MethodInvocationNode miNode = (MethodInvocationNode) node;
    return typeFactory.hasResetMustCall(miNode);
  }

  /**
   * Finds {@link Owning} formal parameters for the method corresponding to a CFG
   *
   * @param cfg the CFG
   * @return
   */
  private Set<ImmutableSet<LocalVarWithTree>> computeOwningParameters(ControlFlowGraph cfg) {
    Set<ImmutableSet<LocalVarWithTree>> init = new LinkedHashSet<>();
    UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
    if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
      // TODO what about lambdas?
      MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
      for (VariableTree param : method.getParameters()) {
        Element paramElement = TreeUtils.elementFromDeclaration(param);
        boolean isMustCallChoice = paramElement.getAnnotation(MustCallChoice.class) != null;
        if (isMustCallChoice
            || (typeFactory.hasMustCall(param)
                && paramElement.getAnnotation(Owning.class) != null)) {
          Set<LocalVarWithTree> setOfLocals = new LinkedHashSet<>();
          setOfLocals.add(
              new LocalVarWithTree(new LocalVariable(paramElement), param, isMustCallChoice));
          init.add(ImmutableSet.copyOf(setOfLocals));
          // Increment numMustCall for each @Owning parameter tracked by the enclosing method
          incrementNumMustCall(param);
        }
      }
    }
    return init;
  }

  /**
   * Checks whether a pair exists in {@code defs} that its first var is equal to {@code node} or
   * not. This is useful when we want to check if a LocalVariableNode is overwritten or not.
   */
  private static boolean isVarInDefs(
      Set<ImmutableSet<LocalVarWithTree>> defs, LocalVariableNode node) {
    return defs.stream()
        .flatMap(Set::stream)
        .map(assign -> ((assign.localVar).getElement()))
        .anyMatch(elem -> elem.equals(node.getElement()));
  }

  private static ImmutableSet<LocalVarWithTree> getSetContainingAssignmentTreeOfVar(
      Set<ImmutableSet<LocalVarWithTree>> defs, LocalVariableNode node) {
    return defs.stream()
        .filter(
            set ->
                set.stream()
                    .anyMatch(assign -> assign.localVar.getElement().equals(node.getElement())))
        .findAny()
        .orElse(null);
  }

  /**
   * Returns a pair in {@code defs} that its first var is equal to {@code node} if one exists, null
   * otherwise.
   */
  private static LocalVarWithTree getAssignmentTreeOfVar(
      Set<ImmutableSet<LocalVarWithTree>> defs, LocalVariableNode node) {
    return defs.stream()
        .flatMap(Set::stream)
        .filter(assign -> assign.localVar.getElement().equals(node.getElement()))
        .findAny()
        .orElse(null);
  }

  /** checks if the variable has been declared in a try-with-resources header */
  private static boolean isTryWithResourcesVariable(LocalVariableNode lhs) {
    Tree tree = lhs.getTree();
    return tree != null
        && TreeUtils.elementFromTree(tree).getKind().equals(ElementKind.RESOURCE_VARIABLE);
  }

  /**
   * Creates the appropriate @CalledMethods annotation that corresponds to the @MustCall annotation
   * declared on the class type of {@code localVarWithTree.first}. Then, it gets @CalledMethod
   * annotation of {@code localVarWithTree.first} to do a subtyping check and reports an error if
   * the check fails.
   */
  private void checkMustCall(
      ImmutableSet<LocalVarWithTree> localVarWithTreeSet,
      CFStore cmStore,
      CFStore mcStore,
      String outOfScopeReason) {

    List<String> mustCallValue = typeFactory.getMustCallValue(localVarWithTreeSet, mcStore);
    // optimization: if there are no must-call methods, we do not need to perform the check
    if (mustCallValue == null || mustCallValue.isEmpty()) {
      return;
    }

    boolean mustCallSatisfied = false;
    for (LocalVarWithTree localVarWithTree : localVarWithTreeSet) {

      // sometimes the store is null!  this looks like a bug in checker dataflow.
      // TODO track down and report the root-cause bug
      CFValue lhsCFValue = cmStore != null ? cmStore.getValue(localVarWithTree.localVar) : null;
      AnnotationMirror cmAnno;

      if (lhsCFValue != null) { // When store contains the lhs
        cmAnno =
            lhsCFValue.getAnnotations().stream()
                .filter(anno -> AnnotationUtils.areSameByClass(anno, CalledMethods.class))
                .findAny()
                .orElse(typeFactory.top);
      } else {
        cmAnno =
            typeFactory
                .getAnnotatedType(localVarWithTree.localVar.getElement())
                .getAnnotationInHierarchy(typeFactory.top);
      }

      if (calledMethodsSatisfyMustCall(mustCallValue, cmAnno)) {
        mustCallSatisfied = true;
        break;
      }
    }

    if (!mustCallSatisfied) {
      if (reportedMustCallErrors.stream()
          .noneMatch(localVarTree -> localVarWithTreeSet.contains(localVarTree))) {
        LocalVarWithTree firstlocalVarWithTree = localVarWithTreeSet.iterator().next();
        if (!checker.shouldSkipUses(TreeUtils.elementFromTree(firstlocalVarWithTree.tree))) {
          reportedMustCallErrors.add(firstlocalVarWithTree);
          checker.reportError(
              firstlocalVarWithTree.tree,
              "required.method.not.called",
              formatMissingMustCallMethods(mustCallValue),
              firstlocalVarWithTree.localVar.getType().toString(),
              outOfScopeReason);
        }
      }
    }
  }

  private void incrementNumMustCall(Tree tree) {
    if (checker.hasOption(ObjectConstructionChecker.COUNT_MUST_CALL)) {
      if (!mustCallObligations.contains(tree)) {
        checker.numMustCall++;
        mustCallObligations.add(tree);
      }
    }
  }

  /**
   * Do the called methods represented by the {@link CalledMethods} type {@code cmAnno} include all
   * the methods in {@code mustCallValue}?
   */
  private boolean calledMethodsSatisfyMustCall(
      List<String> mustCallValue, AnnotationMirror cmAnno) {
    AnnotationMirror cmAnnoForMustCallMethods =
        typeFactory.createCalledMethods(mustCallValue.toArray(new String[0]));
    return typeFactory.getQualifierHierarchy().isSubtype(cmAnno, cmAnnoForMustCallMethods);
  }

  /**
   * Is {@code exceptionClassName} an exception type we are ignoring, to avoid excessive false
   * positives? For now we ignore {@code java.lang.Throwable}, {@code NullPointerException}, and the
   * runtime exceptions that can occur at any point during the program due to something going wrong
   * in the JVM, like OutOfMemoryErrors or ClassCircularityErrors.
   */
  private static boolean isIgnoredExceptionType(@FullyQualifiedName Name exceptionClassName) {
    // any method call has a CFG edge for Throwable/RuntimeException/Error to represent run-time
    // misbehavior. Ignore it.
    return exceptionClassName.contentEquals(Throwable.class.getCanonicalName())
        || exceptionClassName.contentEquals(RuntimeException.class.getCanonicalName())
        || exceptionClassName.contentEquals(Error.class.getCanonicalName())
        // use the Nullness Checker to prove this won't happen
        || exceptionClassName.contentEquals(NullPointerException.class.getCanonicalName())
        // these errors can't be predicted statically, so we'll ignore them and assume they won't
        // happen
        || exceptionClassName.contentEquals(ClassCircularityError.class.getCanonicalName())
        || exceptionClassName.contentEquals(ClassFormatError.class.getCanonicalName())
        || exceptionClassName.contentEquals(NoClassDefFoundError.class.getCanonicalName())
        || exceptionClassName.contentEquals(OutOfMemoryError.class.getCanonicalName())
        // it's not our problem if the Java type system is wrong
        || exceptionClassName.contentEquals(ClassCastException.class.getCanonicalName())
        // it's not our problem if the code is going to divide by zero.
        || exceptionClassName.contentEquals(ArithmeticException.class.getCanonicalName())
        // use the Index Checker to catch the next two cases
        || exceptionClassName.contentEquals(ArrayIndexOutOfBoundsException.class.getCanonicalName())
        || exceptionClassName.contentEquals(NegativeArraySizeException.class.getCanonicalName())
        // Most of the time, this exception is infeasible, as the charset used
        // is guaranteed to be present by the Java spec (e.g., "UTF-8"). Eventually,
        // we could refine this exclusion by looking at the charset being requested
        || exceptionClassName.contentEquals(UnsupportedEncodingException.class.getCanonicalName());
  }

  /**
   * Updates {@code visited} and {@code worklist} if the input {@code state} has not been visited
   * yet.
   */
  private static void propagate(
      BlockWithLocals state, Set<BlockWithLocals> visited, Deque<BlockWithLocals> worklist) {

    if (visited.add(state)) {
      worklist.add(state);
    }
  }

  /**
   * Formats a list of must-call method names to be printed in an error message.
   *
   * @param mustCallVal the list of must-call strings
   * @return a formatted string
   */
  /* package-private */
  static String formatMissingMustCallMethods(List<String> mustCallVal) {
    return mustCallVal.stream().reduce("", (s, acc) -> "".equals(acc) ? s : acc + ", " + s);
  }

  /**
   * A pair of a {@link Block} and a set of {@link LocalVarWithTree}. In our algorithm, a
   * BlockWithLocals represents visiting a {@link Block} while checking the {@link
   * org.checkerframework.checker.mustcall.qual.MustCall} obligations for a set of locals.
   */
  private static class BlockWithLocals {
    public final Block block;
    public final ImmutableSet<ImmutableSet<LocalVarWithTree>> localSetInfo;

    public BlockWithLocals(Block b, Set<ImmutableSet<LocalVarWithTree>> ls) {
      this.block = b;
      this.localSetInfo = ImmutableSet.copyOf(ls);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BlockWithLocals that = (BlockWithLocals) o;
      return block.equals(that.block) && localSetInfo.equals(that.localSetInfo);
    }

    @Override
    public int hashCode() {
      return Objects.hash(block, localSetInfo);
    }
  }

  /**
   * A pair of a local variable along with a tree in the corresponding method that "assigns" the
   * variable. Besides a normal assignment, the tree may be a {@link VariableTree} in the case of a
   * formal parameter. We keep the tree for error-reporting purposes (so we can report an error per
   * assignment to a local, pinpointing the expression whose MustCall may not be satisfied).
   */
  /* package-private */ static class LocalVarWithTree {
    public final LocalVariable localVar;
    public final Tree tree;

    /** true if this is a must-call-choice parameter, which gives it special rules */
    public final boolean isMustCallChoice;

    public LocalVarWithTree(LocalVariable localVarNode, Tree tree) {
      this(localVarNode, tree, false);
    }

    public LocalVarWithTree(LocalVariable localVarNode, Tree tree, boolean isMustCallChoice) {
      this.localVar = localVarNode;
      this.tree = tree;
      this.isMustCallChoice = isMustCallChoice;
    }

    @Override
    public String toString() {
      return "(LocalVarWithAssignTree: localVar: " + localVar + " |||| tree: " + tree + ")";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LocalVarWithTree that = (LocalVarWithTree) o;
      return localVar.equals(that.localVar) && tree.equals(that.tree);
    }

    @Override
    public int hashCode() {
      return Objects.hash(localVar, tree);
    }
  }
}
