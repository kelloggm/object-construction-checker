package org.checkerframework.checker.objectconstruction;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
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
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsBottom;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.objectconstruction.qual.NotOwning;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.BlockImpl;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlockImpl;
import org.checkerframework.dataflow.cfg.block.RegularBlockImpl;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlockImpl;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The annotated type factory for the object construction checker. Primarily responsible for the
 * subtyping rules between @CalledMethod annotations.
 */
public class ObjectConstructionAnnotatedTypeFactory extends CalledMethodsAnnotatedTypeFactory {

  ////
  // fields for @MustCall checking
  ////

  /** By default, should we transfer ownership to the caller when a variable is returned? */
  public boolean transferOwnershipAtReturn = true;

  /** {@code @MustCall} errors reported thus far, to avoid duplicates */
  private final Set<LocalVarWithAssignTree> reportedMustCallErrors = new HashSet<>();

  /**
   * Default constructor matching super. Should be called automatically.
   *
   * @param checker the checker associated with this type factory
   */
  public ObjectConstructionAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return getBundledTypeQualifiers(
        CalledMethods.class, CalledMethodsBottom.class, CalledMethodsPredicate.class);
  }

  /**
   * Creates a @CalledMethods annotation whose values are the given strings.
   *
   * @param val the methods that have been called
   * @return an annotation indicating that the given methods have been called
   */
  public AnnotationMirror createCalledMethods(final String... val) {
    return createAccumulatorAnnotation(Arrays.asList(val));
  }

  @Override
  public void postAnalyze(ControlFlowGraph cfg) {
    if (checker.hasOption(ObjectConstructionChecker.CHECK_MUST_CALL)) {
      mustCallTraverse(cfg);
    }
    super.postAnalyze(cfg);
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
  private void mustCallTraverse(ControlFlowGraph cfg) {
    // add any owning parameters to initial set
    Set<LocalVarWithAssignTree> init = new HashSet<>();
    UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
    if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
      // TODO what about lambdas?
      MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
      for (VariableTree param : method.getParameters()) {
        Element paramElement = TreeUtils.elementFromDeclaration(param);
        if (hasMustCall(param) && paramElement.getAnnotation(Owning.class) != null) {
          init.add(new LocalVarWithAssignTree(new LocalVariable(paramElement), param));
        }
      }
    }
    BlockWithLocals firstBlockLocals = new BlockWithLocals(cfg.getEntryBlock(), init);

    Set<BlockWithLocals> visited = new HashSet<>();
    Deque<BlockWithLocals> worklist = new ArrayDeque<>();

    worklist.add(firstBlockLocals);
    visited.add(firstBlockLocals);

    while (!worklist.isEmpty()) {

      BlockWithLocals curBlockLocals = worklist.removeLast();
      List<Node> nodes = getBlockNodes(curBlockLocals.block);
      Set<LocalVarWithAssignTree> newDefs = new HashSet<>(curBlockLocals.localSetInfo);

      for (Node node : nodes) {

        if (node instanceof MethodInvocationNode) {
          Node receiver = ((MethodInvocationNode) node).getTarget().getReceiver();
          Element method = ((MethodInvocationNode) node).getTarget().getMethod();

          if (receiver instanceof LocalVariableNode
              && isVarInDefs(newDefs, (LocalVariableNode) receiver)) {
            List<String> mustCallVal = getMustCallValue(((LocalVariableNode) receiver).getTree());
            // TODO: I think this will cause false positives if there is more than one value in the
            // @MustCall annotation

            if (mustCallVal.size() == 1
                && mustCallVal.get(0).equals(method.getSimpleName().toString())) {
              // If the method called on the receiver is the same as receiver's @MustCall value,
              // then we can remove the receiver from the newDefs
              newDefs.remove(getAssignmentTreeOfVar(newDefs, (LocalVariableNode) receiver));
            }
          }
        }

        if (node instanceof AssignmentNode) {
          Node lhs = ((AssignmentNode) node).getTarget();
          Node rhs = ((AssignmentNode) node).getExpression();

          if (rhs instanceof TypeCastNode) {
            rhs = ((TypeCastNode) rhs).getOperand();
          }

          Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());

          if (lhsElement.getKind().equals(ElementKind.FIELD) && hasMustCall(lhs.getTree())) {
            if (rhs instanceof LocalVariableNode && isVarInDefs(newDefs, (LocalVariableNode) rhs)) {
              if (getDeclAnnotation(lhsElement, Owning.class) != null) {
                LocalVarWithAssignTree latestAssignmentPair =
                    getAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs);
                newDefs.remove(latestAssignmentPair);
              }
            }
          }

          if (lhs instanceof LocalVariableNode
              && !isTryWithResourcesVariable((LocalVariableNode) lhs)) {

            // Reassignment to the lhs
            if (isVarInDefs(newDefs, (LocalVariableNode) lhs)) {
              LocalVarWithAssignTree latestAssignmentPair =
                  getAssignmentTreeOfVar(newDefs, (LocalVariableNode) lhs);
              checkMustCall(
                  latestAssignmentPair,
                  getStoreBefore(node),
                  "variable overwritten by assignment " + node.getTree());
              newDefs.remove(latestAssignmentPair);
            }

            // If the rhs is an ObjectCreationNode, or a MethodInvocationNode, then it adds
            // the AssignmentNode to the newDefs.
            if ((rhs instanceof ObjectCreationNode)
                || (rhs instanceof MethodInvocationNode && !hasNotOwningAnno(rhs))) {
              newDefs.add(
                  new LocalVarWithAssignTree(
                      new LocalVariable((LocalVariableNode) lhs), node.getTree()));
            }

            // Ownership Transfer
            if (rhs instanceof LocalVariableNode && isVarInDefs(newDefs, (LocalVariableNode) rhs)) {
              // If the rhs is a LocalVariableNode that exists in the newDefs (Note that if a
              // localVariableNode exists in the newDefs it means it isn't assigned to a null
              // literals), then it adds the localVariableNode to the newDefs
              newDefs.add(
                  new LocalVarWithAssignTree(
                      new LocalVariable((LocalVariableNode) lhs), node.getTree()));
              newDefs.remove(getAssignmentTreeOfVar(newDefs, (LocalVariableNode) rhs));
            }
          }
        }

        // Remove the returned localVariableNode from newDefs.
        if (node instanceof ReturnNode
            && (isTransferOwnershipAtReturn(node, cfg)
                || (transferOwnershipAtReturn && !hasNotOwningAnno(node, cfg)))) {
          Node result = ((ReturnNode) node).getResult();
          if (result instanceof LocalVariableNode
              && isVarInDefs(newDefs, (LocalVariableNode) result)) {
            newDefs.remove(getAssignmentTreeOfVar(newDefs, (LocalVariableNode) result));
          }
        }

        if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {
          List<Node> arguments;
          ExecutableElement executableElement;
          if (node instanceof MethodInvocationNode) {
            MethodInvocationNode invocationNode = (MethodInvocationNode) node;
            arguments = invocationNode.getArguments();
            executableElement = TreeUtils.elementFromUse(invocationNode.getTree());
          } else {
            arguments = ((ObjectCreationNode) node).getArguments();
            executableElement = TreeUtils.elementFromUse(((ObjectCreationNode) node).getTree());
          }

          List<? extends VariableElement> formals = executableElement.getParameters();
          if (arguments.size() != formals.size()) {
            // this could happen, e.g., with varargs, or with strange cases like generated Enum
            // constructors
            // for now, just skip this case
            // TODO allow for ownership transfer here if needed in future
            continue;
          }
          for (int i = 0; i < arguments.size(); i++) {
            Node n = arguments.get(i);
            if (n instanceof MethodInvocationNode || n instanceof ObjectCreationNode) {
              VariableElement formal = formals.get(i);
              Set<AnnotationMirror> annotationMirrors = getDeclAnnotations(formal);
              TypeMirror t = TreeUtils.typeOf(n.getTree());
              List<String> mustCallVal = getMustCallValue(n.getTree());
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
                Set<AnnotationMirror> annotationMirrors = getDeclAnnotations(formal);

                if (annotationMirrors.stream()
                    .anyMatch(anno -> AnnotationUtils.areSameByClass(anno, Owning.class))) {
                  // transfer ownership!
                  newDefs.remove(getAssignmentTreeOfVar(newDefs, local));
                }
              }
            }
          }
        }
      }

      if (curBlockLocals.block.getType() == Block.BlockType.EXCEPTION_BLOCK) {
        checkMustCallInExceptionSuccessors(
            (ExceptionBlockImpl) curBlockLocals.block, newDefs, visited, worklist);
      }

      for (BlockImpl succ : getSuccessors(curBlockLocals.block)) {
        Set<LocalVarWithAssignTree> toRemove = new HashSet<>();

        CFStore succRegularStore = this.analysis.getInput(succ).getRegularStore();
        for (LocalVarWithAssignTree assign : newDefs) {

          // If the successor block is the exit block or if the variable is going out of scope
          if (succ instanceof SpecialBlockImpl
              || succRegularStore.getValue(assign.localVar) == null) {
            // technically the variable may be going out of scope before the method exit, but that
            // doesn't seem to provide additional helpful information
            String outOfScopeReason = "regular method exit";
            if (nodes.size() == 0) { // If the cur block is special or conditional block
              checkMustCall(assign, succRegularStore, outOfScopeReason);

            } else { // If the cur block is Exception/Regular block then it checks MustCall
              // annotation in the store right after the last node
              Node last = nodes.get(nodes.size() - 1);
              CFStore storeAfter = getStoreAfter(last);
              checkMustCall(assign, storeAfter, outOfScopeReason);
            }

            toRemove.add(assign);
          }
        }

        newDefs.removeAll(toRemove);
        propagate(new BlockWithLocals(succ, newDefs), visited, worklist);
      }
    }
  }

  /** checks if the variable has been declared in a try-with-resources header */
  private boolean isTryWithResourcesVariable(LocalVariableNode lhs) {
    Tree tree = lhs.getTree();
    return tree != null
        && TreeUtils.elementFromTree(tree).getKind().equals(ElementKind.RESOURCE_VARIABLE);
  }

  boolean hasNotOwningAnno(Node node, ControlFlowGraph cfg) {
    if (node instanceof ReturnNode) {
      UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
      if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
        // TODO: lambdas?
        MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
        ExecutableElement executableElement = TreeUtils.elementFromDeclaration(method);
        return (getDeclAnnotation(executableElement, NotOwning.class) != null);
      }
    }
    return false;
  }

  boolean isTransferOwnershipAtReturn(Node node, ControlFlowGraph cfg) {
    if (node instanceof ReturnNode) {
      UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
      if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
        // TODO: lambdas?
        MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
        ExecutableElement executableElement = TreeUtils.elementFromDeclaration(method);
        return (getDeclAnnotation(executableElement, Owning.class) != null);
      }
    }
    return false;
  }

  boolean hasNotOwningAnno(Node node) {
    if (node instanceof MethodInvocationNode) {
      MethodInvocationTree methodInvocationTree = ((MethodInvocationNode) node).getTree();
      ExecutableElement executableElement = TreeUtils.elementFromUse(methodInvocationTree);
      return (getDeclAnnotation(executableElement, NotOwning.class) != null);
    }
    return false;
  }

  boolean isTransferOwnershipAtMethodInvocation(Node node) {
    if (node instanceof MethodInvocationNode) {
      MethodInvocationTree methodInvocationTree = ((MethodInvocationNode) node).getTree();
      ExecutableElement executableElement = TreeUtils.elementFromUse(methodInvocationTree);
      return (getDeclAnnotation(executableElement, Owning.class) != null);
    }
    return false;
  }

  /**
   * Performs {@code @MustCall} checking for exceptional successors of a block.
   *
   * @param exceptionBlock the block with exceptional successors.
   * @param defs current locals to check
   * @param visited already-visited state
   * @param worklist current worklist
   */
  private void checkMustCallInExceptionSuccessors(
      ExceptionBlock exceptionBlock,
      Set<LocalVarWithAssignTree> defs,
      Set<BlockWithLocals> visited,
      Deque<BlockWithLocals> worklist) {
    Map<TypeMirror, Set<Block>> exSucc = exceptionBlock.getExceptionalSuccessors();
    for (Map.Entry<TypeMirror, Set<Block>> pair : exSucc.entrySet()) {
      if (isIgnoredExceptionType(((Type) pair.getKey()).tsym.getSimpleName())) {
        continue;
      }
      CFStore storeAfter = getStoreAfter(exceptionBlock.getNode());
      for (Block tSucc : pair.getValue()) {
        if (tSucc instanceof SpecialBlock) {
          for (LocalVarWithAssignTree assignTree : defs) {
            checkMustCall(
                assignTree,
                storeAfter,
                "possible exceptional exit due to " + exceptionBlock.getNode().getTree());
          }
        } else {
          propagate(new BlockWithLocals(tSucc, defs), visited, worklist);
        }
      }
    }
  }

  /**
   * Is {@code exceptionClassName} an exception type we are ignoring, to avoid excessive false
   * positives? For now we ignore {@code java.lang.Throwable} and {@code NullPointerException}
   */
  private boolean isIgnoredExceptionType(Name exceptionClassName) {
    boolean isThrowableOrNPE =
        exceptionClassName.contentEquals(Throwable.class.getSimpleName())
            || exceptionClassName.contentEquals(NullPointerException.class.getSimpleName());
    return isThrowableOrNPE;
  }

  /**
   * Returns a pair in {@code defs} that its first var is equal to {@code node} if one exists, null
   * otherwise.
   */
  private @Nullable LocalVarWithAssignTree getAssignmentTreeOfVar(
      Set<LocalVarWithAssignTree> defs, LocalVariableNode node) {
    return defs.stream()
        .filter(assign -> assign.localVar.getElement().equals(node.getElement()))
        .findAny()
        .orElse(null);
  }

  /**
   * Checks whether a pair exists in {@code defs} that its first var is equal to {@code node} or
   * not. This is useful when we want to check if a LocalVariableNode is overwritten or not.
   */
  private boolean isVarInDefs(Set<LocalVarWithAssignTree> defs, LocalVariableNode node) {
    return defs.stream()
        .map(assign -> ((assign.localVar).getElement()))
        .anyMatch(elem -> elem.equals(node.getElement()));
  }

  /**
   * If cur is Conditional block, then it returns a list of two successor blocks contains then block
   * and else block. If cur is instance of SingleSuccessorBlock then it returns a list of one block.
   * Otherwise, it throws an assertion error at runtime.
   *
   * @param cur
   * @return list of successor blocks
   */
  private List<BlockImpl> getSuccessors(BlockImpl cur) {
    List<BlockImpl> successorBlock = new ArrayList<>();

    if (cur.getType() == Block.BlockType.CONDITIONAL_BLOCK) {

      ConditionalBlock ccur = (ConditionalBlock) cur;

      successorBlock.add((BlockImpl) ccur.getThenSuccessor());
      successorBlock.add((BlockImpl) ccur.getElseSuccessor());

    } else {
      if (!(cur instanceof SingleSuccessorBlock)) {
        throw new BugInCF("BlockImpl is neither a conditional block nor a SingleSuccessorBlock");
      }

      Block b = ((SingleSuccessorBlock) cur).getSuccessor();
      if (b != null) {
        successorBlock.add((BlockImpl) b);
      }
    }
    return successorBlock;
  }

  /**
   * If the input block is Regular, the returned list of nodes is exactly the contents of the block.
   * If it's an Exception_Block, then it returns the corresponding node that causes exception.
   * Otherwise, it returns an emptyList.
   *
   * @param block
   * @return List of Nodes
   */
  private List<Node> getBlockNodes(Block block) {
    List<Node> blockNodes = new ArrayList<>();

    switch (block.getType()) {
      case REGULAR_BLOCK:
        blockNodes = ((RegularBlockImpl) block).getContents();
        break;

      case EXCEPTION_BLOCK:
        blockNodes.add(((ExceptionBlockImpl) block).getNode());
    }
    return blockNodes;
  }

  /**
   * Updates {@code visited} and {@code worklist} if the input {@code state} has not been visited
   * yet.
   */
  private void propagate(
      BlockWithLocals state, Set<BlockWithLocals> visited, Deque<BlockWithLocals> worklist) {

    if (visited.add(state)) {
      worklist.add(state);
    }
  }

  /**
   * Returns the String value of @MustCall annotation declared on the class type of {@code tree}.
   */
  List<String> getMustCallValue(Tree tree) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotationMirror mustCallAnnotation =
        mustCallAnnotatedTypeFactory.getAnnotatedType(tree).getAnnotation(MustCall.class);

    return getMustCallValues(mustCallAnnotation);
  }

  /**
   * Returns the String value of @MustCall annotation declared on the class type of {@code element}.
   */
  List<String> getMustCallValue(Element element) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotatedTypeMirror mustCallAnnotatedType =
        mustCallAnnotatedTypeFactory.getAnnotatedType(element);
    AnnotationMirror mustCallAnnotation = mustCallAnnotatedType.getAnnotation(MustCall.class);

    return getMustCallValues(mustCallAnnotation);
  }

  private List<String> getMustCallValues(AnnotationMirror mustCallAnnotation) {
    List<String> mustCallValues =
        (mustCallAnnotation != null)
            ? ValueCheckerUtils.getValueOfAnnotationWithStringArgument(mustCallAnnotation)
            : new ArrayList<>(0);
    return mustCallValues;
  }

  /**
   * Creates the appropriate @CalledMethods annotation that corresponds to the @MustCall annotation
   * declared on the class type of {@code assign.first}. Then, it gets @CalledMethod annotation of
   * {@code assign.first} to do a subtyping check and reports an error if the check fails.
   */
  private void checkMustCall(
      LocalVarWithAssignTree assign, CFStore store, String outOfScopeReason) {
    List<String> mustCallValue = getMustCallValue(assign.assignTree);
    // optimization: if there are no must-call methods, we do not need to perform the check
    if (mustCallValue.isEmpty()) {
      return;
    }
    AnnotationMirror dummyCMAnno = createCalledMethods(mustCallValue.toArray(new String[0]));

    boolean report = true;

    AnnotationMirror cmAnno;

    // sometimes the store is null!  this looks like a bug in checker dataflow.
    // TODO track down and report the root-cause bug
    CFValue lhsCFValue = store != null ? store.getValue(assign.localVar) : null;
    if (lhsCFValue != null) { // When store contains the lhs
      cmAnno =
          lhsCFValue.getAnnotations().stream()
              .filter(anno -> AnnotationUtils.areSameByClass(anno, CalledMethods.class))
              .findAny()
              .orElse(this.top);
    } else {
      cmAnno = getAnnotatedType(assign.localVar.getElement()).getAnnotationInHierarchy(this.top);
    }

    if (this.getQualifierHierarchy().isSubtype(cmAnno, dummyCMAnno)) {
      report = false;
    }

    if (report) {
      if (!reportedMustCallErrors.contains(assign)) {
        reportedMustCallErrors.add(assign);
        checker.reportError(
            assign.assignTree,
            "required.method.not.called",
            formatMissingMustCallMethods(mustCallValue),
            assign.localVar.getType().toString(),
            outOfScopeReason);
      }
    }
  }

  /**
   * Formats a list of must-call method names to be printed in an error message.
   *
   * @param mustCallVal the list of must-call strings
   * @return a formatted string
   */
  String formatMissingMustCallMethods(List<String> mustCallVal) {
    return mustCallVal.stream().reduce("", (s, acc) -> "".equals(acc) ? s : acc + ", " + s);
  }

  boolean hasMustCall(Tree t) {
    return !getMustCallValue(t).isEmpty();
  }

  private static class BlockWithLocals {
    public final BlockImpl block;
    public final Set<LocalVarWithAssignTree> localSetInfo;

    public BlockWithLocals(Block b, Set<LocalVarWithAssignTree> ls) {
      this.block = (BlockImpl) b;
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
   * formal parameter
   */
  private static class LocalVarWithAssignTree {
    public final LocalVariable localVar;
    public final Tree assignTree;

    public LocalVarWithAssignTree(LocalVariable localVarNode, Tree assignTree) {
      this.localVar = localVarNode;
      this.assignTree = assignTree;
    }

    @Override
    public String toString() {
      return "(LocalVarWithAssignTree: localVar: "
          + localVar
          + " |||| assignTree: "
          + assignTree
          + ")";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LocalVarWithAssignTree that = (LocalVarWithAssignTree) o;
      return localVar.equals(that.localVar) && assignTree.equals(that.assignTree);
    }

    @Override
    public int hashCode() {
      return Objects.hash(localVar, assignTree);
    }
  }
}
