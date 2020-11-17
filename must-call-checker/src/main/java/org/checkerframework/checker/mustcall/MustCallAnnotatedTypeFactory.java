package org.checkerframework.checker.mustcall;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static org.checkerframework.common.value.ValueCheckerUtils.getValueOfAnnotationWithStringArgument;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.mustcall.qual.PolyMustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
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
  final AnnotationMirror TOP;

  /** The bottom annotation, which is the default in unannotated code. */
  final AnnotationMirror BOTTOM;

  /** The polymorphic qualifier */
  final AnnotationMirror POLY;

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
    addAliasedAnnotation(InheritableMustCall.class, MustCall.class, true);
    this.postInit();
  }

  @Override
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
  }

  @Override
  public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
    super.addComputedTypeAnnotations(elt, type);
    if (TypesUtils.isPrimitiveOrBoxed(type.getUnderlyingType())) {
      type.replaceAnnotation(BOTTOM);
    }
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
    if (ElementUtils.isClassElement(elt)) {
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
          checker.reportError(
              elt,
              "inconsistent.mustcall.subtype",
              elt.getSimpleName(),
              writtenMCAnno,
              inheritableMustCall);
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
    return new MustCallQualifierHierarchy(this.getSupportedTypeQualifiers(), this.elements);
  }

  /**
   * The qualifier hierarchy is responsible for lub, glb, and subtyping between qualifiers without
   * declaratively-defined subtyping relationships, like our @MustCall annotation.
   */
  private class MustCallQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

    protected MustCallQualifierHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
      super(qualifierClasses, elements);
    }

    @Override
    public AnnotationMirror getTopAnnotation(final AnnotationMirror start) {
      return TOP;
    }

    @Override
    protected AnnotationMirror greatestLowerBoundWithElements(
        AnnotationMirror a1,
        QualifierKind qualifierKind1,
        AnnotationMirror a2,
        QualifierKind qualifierKind2,
        QualifierKind glbKind) {
      Set<String> a1Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a1));
      Set<String> a2Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a2));
      a1Val.retainAll(a2Val);
      return createMustCall(a1Val.toArray(new String[0]));
    }

    @Override
    protected AnnotationMirror leastUpperBoundWithElements(
        AnnotationMirror a1,
        QualifierKind qualifierKind1,
        AnnotationMirror a2,
        QualifierKind qualifierKind2,
        QualifierKind glbKind) {
      Set<String> a1Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a1));
      Set<String> a2Val = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(a2));
      a1Val.addAll(a2Val);
      return createMustCall(a1Val.toArray(new String[0]));
    }

    @Override
    protected boolean isSubtypeWithElements(
        AnnotationMirror subAnno,
        QualifierKind subKind,
        AnnotationMirror superAnno,
        QualifierKind superKind) {
      Set<String> subVal = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(subAnno));
      Set<String> superVal = new LinkedHashSet<>(getValueOfAnnotationWithStringArgument(superAnno));

      return superVal.containsAll(subVal);
    }
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
      /// This code derived from ElementUtils#enclosingClass
      Element enclosing = elt;
      while (enclosing != null && !(isMethodElement(enclosing))) {
        @Nullable Element encl = enclosing.getEnclosingElement();
        enclosing = encl;
      }
      ///
      if (enclosing != null) {
        ExecutableElement enclosingMethod = (ExecutableElement) enclosing;
        VariableElement decl = null;
        for (VariableElement param : enclosingMethod.getParameters()) {
          // scoping rules should mean these are the same? I hope?
          // TODO: find a better way to associate an identifier with a method parameter...
          if (param.getSimpleName().contentEquals(elt.getSimpleName())) {
            decl = param;
            break;
          }
        }
        if (decl != null && getDeclAnnotation(decl, Owning.class) == null) {
          type.replaceAnnotation(BOTTOM);
        }
      }
      return super.visitIdentifier(node, type);
    }

    /** Equivalent to ElementUtils#isClassElement for methods. */
    private boolean isMethodElement(Element elt) {
      return elt.getKind() == METHOD || elt.getKind() == CONSTRUCTOR;
    }
  }
}
