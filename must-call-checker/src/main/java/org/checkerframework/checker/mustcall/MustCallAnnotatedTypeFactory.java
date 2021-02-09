package org.checkerframework.checker.mustcall;

import static javax.lang.model.element.ElementKind.PARAMETER;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallChoice;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.SubtypeIsSubsetQualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The annotated type factory for the must call checker. Primarily responsible for the subtyping
 * rules between @MustCall annotations.
 */
public class MustCallAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The top annotation. */
  public final AnnotationMirror TOP;

  /** The bottom annotation, which is the default in unannotated code. */
  public final AnnotationMirror BOTTOM;

  /** The polymorphic qualifier */
  final AnnotationMirror POLY;

  /**
   * A cache of locations at which an inconsistent.mustcall.subtype error has already been issued,
   * to avoid issuing duplicate errors. Reset with each compilation unit.
   */
  private final Set<Element> elementsIssuedInconsistentMustCallSubtypeErrors =
      new HashSet<>(this.getCacheSize());

  /**
   * Default constructor matching super. Should be called automatically.
   *
   * @param checker the checker associated with this type factory
   */
  public MustCallAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    TOP = AnnotationBuilder.fromClass(elements, MustCallUnknown.class);
    BOTTOM = createMustCall();
    POLY = AnnotationBuilder.fromClass(elements, PolyMustCall.class);
    addAliasedTypeAnnotation(InheritableMustCall.class, MustCall.class, true);
    addAliasedTypeAnnotation(MustCallChoice.class, POLY);
    this.postInit();
  }

  @Override
  public void setRoot(@Nullable CompilationUnitTree root) {
    super.setRoot(root);
    elementsIssuedInconsistentMustCallSubtypeErrors.clear();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    // Because MustCallChoice is in the qual directory, the qualifiers have to be explicitly named
    // or
    // MustCallChoice will be reflectively loaded - making it unavailable as an alias for
    // @PolyMustCall.
    return new LinkedHashSet<>(
        Arrays.asList(MustCall.class, MustCallUnknown.class, PolyMustCall.class));
  }

  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(super.createTreeAnnotator(), new MustCallTreeAnnotator(this));
  }

  @Override
  protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
    super.addComputedTypeAnnotations(tree, type, iUseFlow);
    // All primitives are @MustCall({}). This code is needed to avoid primitive conversions, taking
    // on the MustCall type of an object. For example, without this in this code b's type would be
    // @MustCall("a"), which is nonsensical:
    //
    // @MustCall("a") Object obj; boolean b = obj == null;
    if (TypesUtils.isPrimitiveOrBoxed(type.getUnderlyingType())) {
      type.replaceAnnotation(BOTTOM);
    }
    if (isDeclaredInTryWithResources(TreeUtils.elementFromTree(tree))) {
      type.replaceAnnotation(withoutClose(type.getAnnotationInHierarchy(TOP)));
    }
  }

  @Override
  public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
    super.addComputedTypeAnnotations(elt, type);
    if (TypesUtils.isPrimitiveOrBoxed(type.getUnderlyingType())) {
      type.replaceAnnotation(BOTTOM);
    }
    if (isDeclaredInTryWithResources(elt)) {
      type.replaceAnnotation(withoutClose(type.getAnnotationInHierarchy(TOP)));
    }
  }

  /**
   * Creates a new @MustCall annotation that is identical to the input, but does not have "close".
   * Returns the same annotation mirror if the input annotation didn't have "close" as one of its
   * element.
   *
   * <p>The argument is permitted to be null. If it is null, then bottom is returned.
   *
   * <p>Package private to permit usage from the visitor in the common assignment check.
   */
  /* package-private */ AnnotationMirror withoutClose(@Nullable AnnotationMirror anno) {
    // shortcut for easy paths
    if (anno == null || AnnotationUtils.areSame(anno, BOTTOM)) {
      return BOTTOM;
    } else if (!AnnotationUtils.areSameByClass(anno, MustCall.class)) {
      return anno;
    }
    List<String> values = ValueCheckerUtils.getValueOfAnnotationWithStringArgument(anno);
    if (!values.contains("close")) {
      return anno;
    }
    return createMustCall(values.stream().filter(s -> !"close".equals(s)).toArray(String[]::new));
  }

  /**
   * Returns true iff the given element represents a variable that was declared in a
   * try-with-resources statement.
   *
   * @param elt an element; may be null, in which case this method always returns false
   * @return true iff the given element represents a variable that was declared in a
   *     try-with-resources statement
   */
  private boolean isDeclaredInTryWithResources(@Nullable Element elt) {
    return elt != null && elt.getKind() == ElementKind.RESOURCE_VARIABLE;
  }

  /** Treat non-owning method parameters as @MustCallUnknown when the method is called. */
  @Override
  public void methodFromUsePreSubstitution(ExpressionTree tree, AnnotatedExecutableType type) {
    ExecutableElement declaration;
    if (tree instanceof MethodInvocationTree) {
      declaration = TreeUtils.elementFromUse((MethodInvocationTree) tree);
    } else if (tree instanceof MemberReferenceTree) {
      declaration = (ExecutableElement) TreeUtils.elementFromTree(tree);
    } else {
      throw new BugInCF("unexpected type of method tree: " + tree.getKind());
    }
    changeNonOwningParametersTypes(declaration, type);
    super.methodFromUsePreSubstitution(tree, type);
  }

  @Override
  protected void constructorFromUsePreSubstitution(
      NewClassTree tree, AnnotatedExecutableType type) {
    ExecutableElement declaration = TreeUtils.elementFromUse(tree);
    changeNonOwningParametersTypes(declaration, type);
    super.constructorFromUsePreSubstitution(tree, type);
  }

  /**
   * Changes the type of each parameter not annotated as @Owning to top. Also replaces the component
   * type of the varargs array, if applicable.
   *
   * @param declaration a method or constructor declaration
   * @param type the method or constructor's type
   */
  private void changeNonOwningParametersTypes(
      ExecutableElement declaration, AnnotatedExecutableType type) {
    for (int i = 0; i < type.getParameterTypes().size(); i++) {
      Element paramDecl = declaration.getParameters().get(i);
      if (getDeclAnnotation(paramDecl, Owning.class) == null) {
        AnnotatedTypeMirror paramType = type.getParameterTypes().get(i);
        if (!paramType.hasAnnotation(POLY)) {
          paramType.replaceAnnotation(TOP);
        }
        if (declaration.isVarArgs() && i == type.getParameterTypes().size() - 1) {
          // also modify the last component type of a varargs array
          if (paramType.getKind() == TypeKind.ARRAY) {
            AnnotatedTypeMirror varargsType = ((AnnotatedArrayType) paramType).getComponentType();
            if (!varargsType.hasAnnotation(POLY)) {
              varargsType.replaceAnnotation(TOP);
            }
          }
        }
      }
    }
  }

  @Override
  public AnnotatedTypeMirror fromElement(Element elt) {
    AnnotatedTypeMirror type = super.fromElement(elt);
    // Support @InheritableMustCall meaning @MustCall on all class declaration elements.
    if (ElementUtils.isTypeElement(elt)) {
      AnnotationMirror inheritableMustCall = getDeclAnnotation(elt, InheritableMustCall.class);
      if (inheritableMustCall != null) {
        List<String> mustCallVal =
            ValueCheckerUtils.getValueOfAnnotationWithStringArgument(inheritableMustCall);
        AnnotationMirror inheritedMCAnno = createMustCall(mustCallVal.toArray(new String[0]));
        // Ensure that there isn't an inconsistent, user-written @MustCall annotation and
        // issue an error if there is. Otherwise, replace the implicit @MustCall({}) with
        // the inherited must-call annotation.
        AnnotationMirror writtenMCAnno = type.getAnnotationInHierarchy(this.TOP);
        if (writtenMCAnno != null
            && !this.getQualifierHierarchy().isSubtype(inheritedMCAnno, writtenMCAnno)) {
          if (!elementsIssuedInconsistentMustCallSubtypeErrors.contains(elt)) {
            checker.reportError(
                elt,
                "inconsistent.mustcall.subtype",
                elt.getSimpleName(),
                writtenMCAnno,
                inheritableMustCall);
            elementsIssuedInconsistentMustCallSubtypeErrors.add(elt);
          }
        } else {
          type.replaceAnnotation(inheritedMCAnno);
        }
      }
    }
    return type;
  }

  /**
   * Creates a {@link MustCall} annotation whose values are the given strings.
   *
   * @param val the methods that should be called
   * @return an annotation indicating that the given methods should be called
   */
  public AnnotationMirror createMustCall(final String... val) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MustCall.class);
    Arrays.sort(val);
    builder.setValue("value", val);
    return builder.build();
  }

  @Override
  public QualifierHierarchy createQualifierHierarchy() {
    return new SubtypeIsSubsetQualifierHierarchy(
        this.getSupportedTypeQualifiers(), this.getProcessingEnv());
  }

  /**
   * Fetches the store from the results of dataflow, for either block (if noSuccInfo is true) or
   * succ (if noSuccInfo is false).
   *
   * @param noSuccInfo whether to use the store for the block itself or its successor, succ
   * @param block a block
   * @param succ block's successor
   * @return the appropriate CFStore, populated with MustCall annotations, from the results of
   *     running dataflow
   */
  public CFStore getStoreForBlock(boolean noSuccInfo, Block block, Block succ) {
    return noSuccInfo ? flowResult.getStoreAfter(block) : flowResult.getStoreBefore(succ);
  }

  private class MustCallTreeAnnotator extends TreeAnnotator {
    public MustCallTreeAnnotator(MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory) {
      super(mustCallAnnotatedTypeFactory);
    }

    // When they appear in the body of a method or constructor, treat non-owning parameters
    // as bottom regardless of their declared type.
    @Override
    public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror type) {
      Element elt = TreeUtils.elementFromTree(node);
      if (elt.getKind() == PARAMETER && getDeclAnnotation(elt, Owning.class) == null) {
        type.replaceAnnotation(BOTTOM);
      }
      return super.visitIdentifier(node, type);
    }
  }
}
