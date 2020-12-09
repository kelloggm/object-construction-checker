package org.checkerframework.checker.objectconstruction;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayDeque;
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
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.objectconstruction.qual.NotOwning;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.com.google.common.collect.FluentIterable;
import org.checkerframework.com.google.common.collect.ImmutableSet;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlockImpl;
import org.checkerframework.dataflow.cfg.node.AssignmentContext;
import org.checkerframework.dataflow.cfg.node.AssignmentContext.AssignmentLhsContext;
import org.checkerframework.dataflow.cfg.node.AssignmentContext.LambdaReturnContext;
import org.checkerframework.dataflow.cfg.node.AssignmentContext.MethodParameterContext;
import org.checkerframework.dataflow.cfg.node.AssignmentContext.MethodReturnContext;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
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
        } else if (node instanceof TypeCastNode) {
          handleTypeCast((TypeCastNode) node, newDefs);
        }
      }

      handleSuccessorBlocks(visited, worklist, newDefs, curBlockLocals.block);
    }
  }

  private void handleTypeCast(TypeCastNode node, Set<ImmutableSet<LocalVarWithTree>> defs) {
    Node operand = node.getOperand();
    if (operand instanceof MethodInvocationNode || operand instanceof ObjectCreationNode) {
      if (!shouldSkipInvokePseudoAssignCheck(operand, defs)) {
        checkPseudoAssignToOwning(node);
      }
    }
  }

  private void handleInvocation(Set<ImmutableSet<LocalVarWithTree>> newDefs, Node node) {
    doOwnershipTransferToParameters(newDefs, node);
    // If the method call is nested in a type cast, we won't have a proper AssignmentContext for
    // checking.  So we defer the check to the corresponding TypeCastNode
    if (!nestedInTypeCast(node) && !shouldSkipInvokePseudoAssignCheck(node, newDefs)) {
      // If the node is not skipped by the shouldSkipInvokePseudoAssignCheck() it means we have a
      // method invocation or object creation node that creates a new resource, so we increment
      // the numMustCall
      incrementNumMustCall();
      checkPseudoAssignToOwning(node);
    }
  }

  /**
   * Given a node representing a method or constructor call, checks that if the call has a non-empty
   * {@code @MustCall} type, then its result is pseudo-assigned to some location that can take
   * ownership of the result
   */
  private void checkPseudoAssignToOwning(Node node) {
    Tree tree = node.getTree();
    List<String> mustCallVal = typeFactory.getMustCallValue(tree);
    if (mustCallVal.isEmpty()) {
      return;
    }
    // Default to assuming that non-source nodes will always be assigned to
    // an owning location, but that source nodes will not. This allows the checker
    // to correctly handle foreach loops.
    boolean assignedToOwning = !node.getInSource();
    AssignmentContext assignmentContext = node.getAssignmentContext();
    if (assignmentContext != null) {
      Element elementForType = assignmentContext.getElementForType();
      if (assignmentContext instanceof AssignmentLhsContext) {
        // lhs should be a local variable
        assignedToOwning = isOwningAssignmentLhs(elementForType);
      } else if (assignmentContext instanceof MethodParameterContext) {
        // must be an @Owning or @MustCallChoice parameter
        assignedToOwning =
            typeFactory.getDeclAnnotation(elementForType, Owning.class) != null
                || typeFactory.hasMustCallChoice(elementForType);
      } else if (assignmentContext instanceof MethodReturnContext) {
        // must be an @Owning return
        assignedToOwning =
            TRANSFER_OWNERSHIP_AT_RETURN
                ? typeFactory.getDeclAnnotation(
                        TreeUtils.elementFromTree(assignmentContext.getContextTree()),
                        NotOwning.class)
                    == null
                : typeFactory.getDeclAnnotation(elementForType, Owning.class) != null;
      } else if (assignmentContext instanceof LambdaReturnContext) {
        // TODO handle this case.  For now we will report an error
      } else {
        throw new BugInCF("unexpected AssignmentContext type " + assignmentContext.getClass());
      }
    }
    if (!assignedToOwning) {
      // check if @CalledMethods type of return satisfies the @MustCall obligation
      AnnotationMirror cmAnno =
          typeFactory.getAnnotatedType(tree).getAnnotationInHierarchy(typeFactory.top);
      if (!calledMethodsSatisfyMustCall(mustCallVal, cmAnno)) {
        checker.reportError(
            tree,
            "required.method.not.called",
            MustCallInvokedChecker.formatMissingMustCallMethods(mustCallVal),
            TreeUtils.typeOf(tree).toString(),
            "never assigned to an @Owning location");
      }
    }
  }

  /**
   * Does an element represents an assignment left-hand side that can take ownership?
   *
   * @param elem the element
   * @return {@code true} iff {@code elem} represents a local variable, a try-with-resources
   *     variable, or an {@code @Owning} field
   */
  private boolean isOwningAssignmentLhs(Element elem) {
    return elem != null
        && (elem.getKind().equals(ElementKind.LOCAL_VARIABLE)
            || elem.getKind().equals(ElementKind.RESOURCE_VARIABLE)
            || (elem.getKind().equals(ElementKind.FIELD)
                && typeFactory.getDeclAnnotation(elem, Owning.class) != null));
  }

  /**
   * Checks for cases where we do not need to ensure that a method invocation gets pseudo-assigned
   * to a variable / field that takes ownership. We can skip the check when the invoked method's
   * return type is {@link org.checkerframework.common.returnsreceiver.qual.This}, the invocation is
   * a super constructor call, the method's return type is annotated {@link NotOwning}, or the
   * receiver parameter is in the {@code defs} and has @MustCallChoice annotation.
   */
  private boolean shouldSkipInvokePseudoAssignCheck(
      Node node, Set<ImmutableSet<LocalVarWithTree>> defs) {
    Tree callTree = node.getTree();
    List<String> mustCallVal = typeFactory.getMustCallValue(callTree);
    if (mustCallVal.isEmpty()) {
      return true;
    }
    // In this case, we are handling method invocation or object creation node that has
    // MustCallChoice annotation and the local variable passed as the @MustCallChoice parameter is
    // in the defs
    if (callTree.getKind() == Tree.Kind.METHOD_INVOCATION
        || callTree.getKind() == Tree.Kind.NEW_CLASS) {
      LocalVariableNode mustCallChoiceParam = getLocalPassedAsMustCallChoiceParam(node);
      if (mustCallChoiceParam != null) {
        return isVarInDefs(defs, mustCallChoiceParam);
      }
    }
    if (callTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree methodInvokeTree = (MethodInvocationTree) callTree;

      return typeFactory.returnsThis(methodInvokeTree)
          || TreeUtils.isSuperConstructorCall(methodInvokeTree)
          || TreeUtils.isThisConstructorCall(methodInvokeTree)
          || typeFactory.getDeclAnnotation(
                  TreeUtils.elementFromUse(methodInvokeTree), NotOwning.class)
              != null;
    }
    return false;
  }

  /**
   * Checks if {@code node} is an invocation nested inside a TypeCastNode, by looking at the
   * successor block in the CFG
   */
  private boolean nestedInTypeCast(Node node) {
    if (!(node instanceof MethodInvocationNode || node instanceof ObjectCreationNode)) {
      throw new BugInCF("unexpected node type " + node.getClass());
    }
    if (!(node.getBlock() instanceof ExceptionBlock)) {
      // can happen, e.g., for calls generated for enhanced for loops
      return false;
    }
    Block successorBlock = ((ExceptionBlock) node.getBlock()).getSuccessor();
    if (successorBlock instanceof ExceptionBlock) {
      Node succNode = ((ExceptionBlock) successorBlock).getNode();
      return succNode instanceof TypeCastNode
          && ((TypeCastNode) succNode).getOperand().equals(node);
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
      if (n instanceof LocalVariableNode) {
        LocalVariableNode local = (LocalVariableNode) n;
        if (isVarInDefs(newDefs, local)) {

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
  }

  private void handleReturn(
      ReturnNode node, ControlFlowGraph cfg, Set<ImmutableSet<LocalVarWithTree>> newDefs) {
    if (isTransferOwnershipAtReturn(cfg)) {
      Node result = node.getResult();
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
    Node lhs = node.getTarget();
    Node rhs = node.getExpression();

    while (rhs instanceof TypeCastNode) {
      rhs = ((TypeCastNode) rhs).getOperand();
    }

    Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());

    // Ownership transfer to @Owning field
    if (lhsElement.getKind().equals(ElementKind.FIELD) && typeFactory.hasMustCall(lhs.getTree())) {
      if (rhs instanceof LocalVariableNode && isVarInDefs(newDefs, (LocalVariableNode) rhs)) {
        if (typeFactory.getDeclAnnotation(lhsElement, Owning.class) != null) {
          Set<LocalVarWithTree> setContainingLatestAssignmentPair =
              getSetContainingAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs);
          newDefs.remove(setContainingLatestAssignmentPair);
        }
      }
    } else if (lhs instanceof LocalVariableNode
        && !isTryWithResourcesVariable((LocalVariableNode) lhs)) {

      // Reassignment to the lhs
      if (isVarInDefs(newDefs, (LocalVariableNode) lhs)) {
        ImmutableSet<LocalVarWithTree> setContainingLatestAssignmentPair =
            getSetContainingAssignmentTreeOfVar(newDefs, (LocalVariableNode) lhs);
        LocalVarWithTree latestAssignmentPair =
            getAssignmentTreeOfVar(newDefs, (LocalVariableNode) lhs);
        if (setContainingLatestAssignmentPair.size() == 1) {
          checkMustCall(
              setContainingLatestAssignmentPair,
              typeFactory.getStoreBefore(node),
              "variable overwritten by assignment " + node.getTree());
          newDefs.remove(setContainingLatestAssignmentPair);
        } else {
          ImmutableSet<LocalVarWithTree> newSetContainingLatestAssignmentPair =
              FluentIterable.from(setContainingLatestAssignmentPair)
                  .filter(assignment -> assignment.equals(latestAssignmentPair))
                  .toSet();
          newDefs.remove(setContainingLatestAssignmentPair);
          newDefs.add(newSetContainingLatestAssignmentPair);
        }
      }

      // If the rhs is an ObjectCreationNode, or a MethodInvocationNode, then it adds
      // the AssignmentNode to the newDefs.
      if ((rhs instanceof ObjectCreationNode)
          || (rhs instanceof MethodInvocationNode
              && !hasNotOwningReturnType((MethodInvocationNode) rhs))) {
        LocalVariableNode mustCallChoiceParamLocal = getLocalPassedAsMustCallChoiceParam(rhs);
        if (mustCallChoiceParamLocal != null) {
          if (isVarInDefs(newDefs, mustCallChoiceParamLocal)) {
            ImmutableSet<LocalVarWithTree> immutableSet =
                getSetContainingAssignmentTreeOfVar(newDefs, mustCallChoiceParamLocal);
            ImmutableSet<LocalVarWithTree> newImmutableSet =
                FluentIterable.from(immutableSet)
                    .append(
                        new LocalVarWithTree(
                            new LocalVariable((LocalVariableNode) lhs), node.getTree()))
                    .toSet();
            newDefs.remove(immutableSet);
            newDefs.add(newImmutableSet);
          }
        } else {
          LocalVarWithTree lhsLocalVarWithTreeNew =
              new LocalVarWithTree(new LocalVariable((LocalVariableNode) lhs), node.getTree());
          newDefs.add(ImmutableSet.of(lhsLocalVarWithTreeNew));
        }
      }

      // Ownership Transfer
      if (rhs instanceof LocalVariableNode && isVarInDefs(newDefs, (LocalVariableNode) rhs)) {
        // If the rhs is a LocalVariableNode that exists in the newDefs (Note that if a
        // localVariableNode exists in the newDefs it means it isn't assigned to a null
        // literals), then it adds the localVariableNode to the newDefs
        ImmutableSet<LocalVarWithTree> setContainingRhs =
            getSetContainingAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs);
        LocalVarWithTree lhsLocalVarWithTreeNew =
            new LocalVarWithTree(new LocalVariable((LocalVariableNode) lhs), node.getTree());
        newDefs.add(ImmutableSet.of(lhsLocalVarWithTreeNew));
        newDefs.remove(setContainingRhs);
      }
    }
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
  private @Nullable LocalVariableNode getLocalPassedAsMustCallChoiceParam(Node node) {
    while (node instanceof TypeCastNode) {
      node = ((TypeCastNode) node).getOperand();
    }

    if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {

      if (!typeFactory.hasMustCallChoice(node.getTree())) {
        return null;
      }

      List<Node> arguments = getArgumentsOfMethodOrConstructor(node);
      List<? extends VariableElement> formals = getFormalsOfMethodOrConstructor(node);

      for (int i = 0; i < arguments.size(); i++) {
        if (!typeFactory.hasMustCallChoice(formals.get(i))) {
          continue;
        }
        Node n = arguments.get(i);
        return getLocalPassedAsMustCallChoiceParam(n);
      }
      // If node does't have @MustCallChoice parameter then it checks the receiver parameter
      if (node instanceof MethodInvocationNode) {
        Node n = ((MethodInvocationNode) node).getTarget().getReceiver();
        return getLocalPassedAsMustCallChoiceParam(n);
      }
    }

    return (node != null && node instanceof LocalVariableNode) ? (LocalVariableNode) node : null;
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
    return (typeFactory.getDeclAnnotation(executableElement, NotOwning.class) != null);
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
        if (succ instanceof SpecialBlockImpl
            || setAssign.stream()
                .allMatch(assign -> succRegularStore.getValue(assign.localVar) == null)) {
          if (nodes.size() == 0) { // If the cur block is special or conditional block
            checkMustCall(setAssign, succRegularStore, reasonForSucc);

          } else { // If the cur block is Exception/Regular block then it checks MustCall
            // annotation in the store right after the last node
            Node last = nodes.get(nodes.size() - 1);
            CFStore storeAfter = typeFactory.getStoreAfter(last);
            checkMustCall(setAssign, storeAfter, reasonForSucc);
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
        if (typeFactory.hasMustCall(param) && paramElement.getAnnotation(Owning.class) != null) {
          Set<LocalVarWithTree> setOfLocals = new LinkedHashSet<>();
          setOfLocals.add(new LocalVarWithTree(new LocalVariable(paramElement), param));
          init.add(ImmutableSet.copyOf(setOfLocals));
          // Increment numMustCall for each @Owning parameter tracked by the enclosing method
          incrementNumMustCall();
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
      ImmutableSet<LocalVarWithTree> localVarWithTreeSet, CFStore store, String outOfScopeReason) {

    List<String> mustCallValue =
        typeFactory.getMustCallValue(localVarWithTreeSet.iterator().next().tree);
    // optimization: if there are no must-call methods, we do not need to perform the check
    if (mustCallValue.isEmpty()) {
      return;
    }

    boolean mustCallSatisfied = false;
    for (LocalVarWithTree localVarWithTree : localVarWithTreeSet) {

      if (typeFactory.isUnconnectedSocket(localVarWithTree.tree)) {
        return;
      }

      // sometimes the store is null!  this looks like a bug in checker dataflow.
      // TODO track down and report the root-cause bug
      CFValue lhsCFValue = store != null ? store.getValue(localVarWithTree.localVar) : null;
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

  private void incrementNumMustCall() {
    if (checker.hasOption(ObjectConstructionChecker.COUNT_MUST_CALL)) {
      checker.numMustCall++;
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
   * positives? For now we ignore {@code java.lang.Throwable} and {@code NullPointerException}
   */
  private static boolean isIgnoredExceptionType(@FullyQualifiedName Name exceptionClassName) {
    boolean isThrowableOrNPE =
        exceptionClassName.contentEquals(Throwable.class.getCanonicalName())
            || exceptionClassName.contentEquals(NullPointerException.class.getCanonicalName());
    return isThrowableOrNPE;
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
  private static class LocalVarWithTree {
    public final LocalVariable localVar;
    public final Tree tree;

    public LocalVarWithTree(LocalVariable localVarNode, Tree tree) {
      this.localVar = localVarNode;
      this.tree = tree;
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
