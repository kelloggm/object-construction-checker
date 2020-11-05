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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.objectconstruction.qual.NotOwning;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlockImpl;
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

  private final BaseTypeChecker checker;

  private final CFAnalysis analysis;

  /* package-private */
  MustCallInvokedChecker(
      ObjectConstructionAnnotatedTypeFactory typeFactory,
      BaseTypeChecker checker,
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

    Set<BlockWithLocals> visited = new HashSet<>();
    Deque<BlockWithLocals> worklist = new ArrayDeque<>();

    worklist.add(firstBlockLocals);
    visited.add(firstBlockLocals);

    while (!worklist.isEmpty()) {

      BlockWithLocals curBlockLocals = worklist.removeLast();
      List<Node> nodes = curBlockLocals.block.getNodes();
      // defs to be tracked in successor blocks, updated by code below
      Set<LocalVarWithTree> newDefs = new HashSet<>(curBlockLocals.localSetInfo);

      for (Node node : nodes) {

        if (node instanceof AssignmentNode) {
          handleAssignment((AssignmentNode) node, newDefs);
        } else if (node instanceof ReturnNode) {
          handleReturn((ReturnNode) node, cfg, newDefs);
        } else if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {
          handleInvocation(newDefs, node);
        }
      }

      handleSuccessorBlocks(visited, worklist, newDefs, curBlockLocals.block);
    }
  }

  private void handleInvocation(Set<LocalVarWithTree> newDefs, Node node) {
    List<Node> arguments;
    ExecutableElement executableElement;
    if (node instanceof MethodInvocationNode) {
      MethodInvocationNode invocationNode = (MethodInvocationNode) node;
      arguments = invocationNode.getArguments();
      executableElement = TreeUtils.elementFromUse(invocationNode.getTree());
    } else {
      if (!(node instanceof ObjectCreationNode)) {
        throw new BugInCF("unexpected node type " + node.getClass());
      }
      arguments = ((ObjectCreationNode) node).getArguments();
      executableElement = TreeUtils.elementFromUse(((ObjectCreationNode) node).getTree());
    }

    List<? extends VariableElement> formals = executableElement.getParameters();
    if (arguments.size() != formals.size()) {
      // this could happen, e.g., with varargs, or with strange cases like generated Enum
      // constructors
      // for now, just skip this case
      // TODO allow for ownership transfer here if needed in future
      return;
    }
    for (int i = 0; i < arguments.size(); i++) {
      Node n = arguments.get(i);
      if (n instanceof MethodInvocationNode || n instanceof ObjectCreationNode) {
        VariableElement formal = formals.get(i);
        Set<AnnotationMirror> annotationMirrors = typeFactory.getDeclAnnotations(formal);
        TypeMirror t = TreeUtils.typeOf(n.getTree());
        List<String> mustCallVal = typeFactory.getMustCallValue(n.getTree());
        if (!mustCallVal.isEmpty()
            && annotationMirrors.stream()
                .noneMatch(anno -> AnnotationUtils.areSameByClass(anno, Owning.class))) {
          // TODO why is this logic here and not in the visitor?
          checker.reportError(
              n.getTree(),
              "required.method.not.called",
              formatMissingMustCallMethods(mustCallVal),
              t.toString(),
              "never assigned to a variable");
        }
      }

      if (n instanceof LocalVariableNode) {
        LocalVariableNode local = (LocalVariableNode) n;
        if (isVarInDefs(newDefs, local)) {

          // check if formal has an @Owning annotation
          VariableElement formal = formals.get(i);
          Set<AnnotationMirror> annotationMirrors = typeFactory.getDeclAnnotations(formal);

          if (annotationMirrors.stream()
              .anyMatch(anno -> AnnotationUtils.areSameByClass(anno, Owning.class))) {
            // transfer ownership!
            newDefs.remove(getAssignmentTreeOfVar(newDefs, local));
          }
        }
      }
    }
  }

  private void handleReturn(ReturnNode node, ControlFlowGraph cfg, Set<LocalVarWithTree> newDefs) {
    if (isTransferOwnershipAtReturn(cfg)) {
      Node result = node.getResult();
      if (result instanceof LocalVariableNode && isVarInDefs(newDefs, (LocalVariableNode) result)) {
        newDefs.remove(getAssignmentTreeOfVar(newDefs, (LocalVariableNode) result));
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

  private void handleAssignment(AssignmentNode node, Set<LocalVarWithTree> newDefs) {
    Node lhs = node.getTarget();
    Node rhs = node.getExpression();

    if (rhs instanceof TypeCastNode) {
      rhs = ((TypeCastNode) rhs).getOperand();
    }

    if (lhs instanceof LocalVariableNode && !isTryWithResourcesVariable((LocalVariableNode) lhs)) {

      // Reassignment to the lhs
      if (isVarInDefs(newDefs, (LocalVariableNode) lhs)) {
        LocalVarWithTree latestAssignmentPair =
            getAssignmentTreeOfVar(newDefs, (LocalVariableNode) lhs);
        checkMustCall(
            latestAssignmentPair,
            typeFactory.getStoreBefore(node),
            "variable overwritten by assignment " + node.getTree());
        newDefs.remove(latestAssignmentPair);
      }

      // If the rhs is an ObjectCreationNode, or a MethodInvocationNode, then it adds
      // the AssignmentNode to the newDefs.
      if ((rhs instanceof ObjectCreationNode)
          || (rhs instanceof MethodInvocationNode
              && !hasNotOwningReturnType((MethodInvocationNode) rhs))) {
        newDefs.add(
            new LocalVarWithTree(new LocalVariable((LocalVariableNode) lhs), node.getTree()));
      }

      // Ownership Transfer
      if (rhs instanceof LocalVariableNode && isVarInDefs(newDefs, (LocalVariableNode) rhs)) {
        // If the rhs is a LocalVariableNode that exists in the newDefs (Note that if a
        // localVariableNode exists in the newDefs it means it isn't assigned to a null
        // literals), then it adds the localVariableNode to the newDefs
        newDefs.add(
            new LocalVarWithTree(new LocalVariable((LocalVariableNode) lhs), node.getTree()));
        newDefs.remove(getAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs));
      }
    }
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
   * @return set of relevant successors for block
   */
  private Set<Block> getRelevantSuccessors(Block block) {
    if (block.getType() == Block.BlockType.EXCEPTION_BLOCK) {
      ExceptionBlock excBlock = (ExceptionBlock) block;
      Set<Block> result = new LinkedHashSet<>();
      // regular successor
      Block regularSucc = excBlock.getSuccessor();
      if (regularSucc != null) {
        result.add(regularSucc);
      }
      // relevant exception successors
      Map<TypeMirror, Set<Block>> exSucc = excBlock.getExceptionalSuccessors();
      for (Map.Entry<TypeMirror, Set<Block>> pair : exSucc.entrySet()) {
        if (!isIgnoredExceptionType(((Type) pair.getKey()).tsym.getQualifiedName())) {
          result.addAll(pair.getValue());
        }
      }
      return result;
    } else {
      return block.getSuccessors();
    }
  }

  private void handleSuccessorBlocks(
      Set<BlockWithLocals> visited,
      Deque<BlockWithLocals> worklist,
      Set<LocalVarWithTree> defs,
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
    for (Block succ : getRelevantSuccessors(block)) {
      Set<LocalVarWithTree> defsCopy = new HashSet<>(defs);
      Set<LocalVarWithTree> toRemove = new HashSet<>();

      CFStore succRegularStore = analysis.getInput(succ).getRegularStore();
      for (LocalVarWithTree assign : defs) {

        // If the successor block is the exit block or if the variable is going out of scope
        if (succ instanceof SpecialBlockImpl
            || succRegularStore.getValue(assign.localVar) == null) {
          if (nodes.size() == 0) { // If the cur block is special or conditional block
            checkMustCall(assign, succRegularStore, outOfScopeReason);

          } else { // If the cur block is Exception/Regular block then it checks MustCall
            // annotation in the store right after the last node
            Node last = nodes.get(nodes.size() - 1);
            CFStore storeAfter = typeFactory.getStoreAfter(last);
            checkMustCall(assign, storeAfter, outOfScopeReason);
          }

          toRemove.add(assign);
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
  private Set<LocalVarWithTree> computeOwningParameters(ControlFlowGraph cfg) {
    Set<LocalVarWithTree> init = new HashSet<>();
    UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
    if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
      // TODO what about lambdas?
      MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
      for (VariableTree param : method.getParameters()) {
        Element paramElement = TreeUtils.elementFromDeclaration(param);
        if (typeFactory.hasMustCall(param) && paramElement.getAnnotation(Owning.class) != null) {
          init.add(new LocalVarWithTree(new LocalVariable(paramElement), param));
        }
      }
    }
    return init;
  }

  /**
   * Checks whether a pair exists in {@code defs} that its first var is equal to {@code node} or
   * not. This is useful when we want to check if a LocalVariableNode is overwritten or not.
   */
  private static boolean isVarInDefs(Set<LocalVarWithTree> defs, LocalVariableNode node) {
    return defs.stream()
        .map(assign -> ((assign.localVar).getElement()))
        .anyMatch(elem -> elem.equals(node.getElement()));
  }

  /**
   * Returns a pair in {@code defs} that its first var is equal to {@code node} if one exists, null
   * otherwise.
   */
  private static LocalVarWithTree getAssignmentTreeOfVar(
      Set<LocalVarWithTree> defs, LocalVariableNode node) {
    return defs.stream()
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
      LocalVarWithTree localVarWithTree, CFStore store, String outOfScopeReason) {
    List<String> mustCallValue = typeFactory.getMustCallValue(localVarWithTree.tree);
    // optimization: if there are no must-call methods, we do not need to perform the check
    if (mustCallValue.isEmpty()) {
      return;
    }
    AnnotationMirror cmAnnoForMustCallMethods =
        typeFactory.createCalledMethods(mustCallValue.toArray(new String[0]));

    AnnotationMirror cmAnno;

    // sometimes the store is null!  this looks like a bug in checker dataflow.
    // TODO track down and report the root-cause bug
    CFValue lhsCFValue = store != null ? store.getValue(localVarWithTree.localVar) : null;
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

    if (!typeFactory.getQualifierHierarchy().isSubtype(cmAnno, cmAnnoForMustCallMethods)) {
      if (!reportedMustCallErrors.contains(localVarWithTree)) {
        reportedMustCallErrors.add(localVarWithTree);

        checker.reportError(
            localVarWithTree.tree,
            "required.method.not.called",
            formatMissingMustCallMethods(mustCallValue),
            localVarWithTree.localVar.getType().toString(),
            outOfScopeReason);
      }
    }
  }

  /**
   * Is {@code exceptionClassName} an exception type we are ignoring, to avoid excessive false
   * positives? For now we ignore {@code java.lang.Throwable} and {@code NullPointerException}
   */
  private static boolean isIgnoredExceptionType(Name exceptionClassName) {
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
    public final Set<LocalVarWithTree> localSetInfo;

    public BlockWithLocals(Block b, Set<LocalVarWithTree> ls) {
      this.block = b;
      this.localSetInfo = ls;
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
