package org.checkerframework.checker.objectconstruction;

import static javax.lang.model.element.ElementKind.METHOD;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;
import org.checkerframework.checker.calledmethods.CalledMethodsVisitor;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.qual.CreatesObligation;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class ObjectConstructionVisitor extends CalledMethodsVisitor {

  private @MonotonicNonNull ObjectConstructionAnnotatedTypeFactory atypeFactory;

  /** @param checker the type-checker associated with this visitor */
  public ObjectConstructionVisitor(final BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  protected ObjectConstructionAnnotatedTypeFactory createTypeFactory() {
    atypeFactory = new ObjectConstructionAnnotatedTypeFactory(checker);
    return atypeFactory;
  }

  /**
   * Issue an error at every EnsuresCalledMethodsVarArgs annotation, because using it is unsound.
   */
  @Override
  public Void visitAnnotation(final AnnotationTree node, final Void p) {
    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(node);
    if (AnnotationUtils.areSameByName(
        anno, "org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethodsVarArgs")) {
      // we can't verify these yet.  emit an error (which will have to be suppressed) for now
      checker.report(node, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.unverified"));
      return null;
    }
    return super.visitAnnotation(node, p);
  }

  @Override
  public Void visitMethod(MethodTree node, Void p) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(node);
    AnnotationMirror ecmva = atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethodsVarArgs.class);
    if (ecmva != null) {
      if (!elt.isVarArgs()) {
        checker.report(
            node, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.annotation.invalid"));
        return null;
      }
    }
    MustCallAnnotatedTypeFactory mcAtf =
        atypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
    List<String> coValues = getCOValues(elt, mcAtf, atypeFactory);
    if (!coValues.isEmpty()) {
      // Check the validity of the annotation, by ensuring that if this method is overriding another
      // method
      // it also creates at least as many obligations. Without this check, dynamic dispatch might
      // allow e.g. a field to
      // be overwritten by a CO method, but the CO effect wouldn't occur.
      for (ExecutableElement overridden : ElementUtils.getOverriddenMethods(elt, this.types)) {
        List<String> overriddenCoValues = getCOValues(overridden, mcAtf, atypeFactory);
        if (!overriddenCoValues.containsAll(coValues)) {
          String foundCoValueString = String.join(", ", coValues);
          String neededCoValueString = String.join(", ", overriddenCoValues);
          String actualClassname = ElementUtils.getEnclosingClassName(elt);
          String overriddenClassname = ElementUtils.getEnclosingClassName(overridden);
          checker.reportError(
              node,
              "creates.obligation.override.invalid",
              actualClassname + "#" + elt,
              overriddenClassname + "#" + overridden,
              foundCoValueString,
              neededCoValueString);
        }
      }
    }
    return super.visitMethod(node, p);
  }

  /**
   * Returns the literal string present in the given @CreatesObligation annotation, or "this" if
   * there is none.
   *
   * @param createsObligation an @CreatesObligation annotation
   * @param mcAtf a MustCallAnnotatedTypeFactory, to source the value element
   * @return the string value
   */
  private static String getCOValue(
      AnnotationMirror createsObligation, MustCallAnnotatedTypeFactory mcAtf) {
    return AnnotationUtils.getElementValue(
        createsObligation, mcAtf.createsObligationValueElement, String.class, "this");
  }

  /**
   * Returns all the literal strings present in the @CreatesObligation annotations on the given
   * element. This version correctly handles multiple CreatesObligation annotations on the same
   * element.
   *
   * @param elt an executable element
   * @param mcAtf a MustCallAnnotatedTypeFactory, to source the value element
   * @param atypeFactory a ObjectConstructionAnnotatedTypeFactory
   * @return the literal strings present in the @CreatesObligation annotation(s) of that element,
   *     substituting the default "this" for empty annotations. This method returns the empty list
   *     iff there are no @CreatesObligation annotations on elt. The returned list is always
   *     modifiable if it is non-empty.
   */
  /*package-private*/ static List<String> getCOValues(
      ExecutableElement elt,
      MustCallAnnotatedTypeFactory mcAtf,
      ObjectConstructionAnnotatedTypeFactory atypeFactory) {
    AnnotationMirror createsObligationList =
        atypeFactory.getDeclAnnotation(elt, CreatesObligation.List.class);
    if (createsObligationList != null) {
      List<AnnotationMirror> createObligations =
          AnnotationUtils.getElementValueArray(
              createsObligationList,
              mcAtf.createsObligationListValueElement,
              AnnotationMirror.class);
      List<String> result = new ArrayList<>();
      for (AnnotationMirror co : createObligations) {
        result.add(getCOValue(co, mcAtf));
      }
      return result;
    }
    AnnotationMirror createsObligation =
        atypeFactory.getDeclAnnotation(elt, CreatesObligation.class);
    if (createsObligation != null) {
      // don't use Collections.singletonList because it's not guaranteed to be mutable
      List<String> result = new ArrayList<>(1);
      result.add(getCOValue(createsObligation, mcAtf));
      return result;
    }
    return Collections.emptyList();
  }

  @Override
  public Void visitVariable(VariableTree node, Void p) {
    Element varElement = TreeUtils.elementFromTree(node);

    if (varElement.getKind().isField()
        && !checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
        && atypeFactory.getDeclAnnotation(varElement, Owning.class) != null) {
      checkOwningField(varElement);
    }

    return super.visitVariable(node, p);
  }

  /**
   * Checks validity of a final field {@code field} with an {@code @Owning} annotation. Say the type
   * of {@code field} is {@code @MustCall("m"}}. This method checks that the enclosing class of
   * {@code field} has a type {@code @MustCall("m2")} for some method {@code m2}, and that {@code
   * m2} has an annotation {@code @EnsuresCalledMethods(value = "this.field", methods = "m")},
   * guaranteeing that the {@code @MustCall} obligation of the field will be satisfied.
   */
  private void checkOwningField(Element field) {

    if (checker.shouldSkipUses(field)) {
      return;
    }

    List<String> fieldMCAnno = atypeFactory.getMustCallValue(field);
    String error = "";

    if (!fieldMCAnno.isEmpty()) {
      Element enclosingElement = field.getEnclosingElement();
      List<String> enclosingMCAnno = atypeFactory.getMustCallValue(enclosingElement);

      if (enclosingMCAnno != null) {
        List<? extends Element> classElements = enclosingElement.getEnclosedElements();
        for (Element element : classElements) {
          if (fieldMCAnno.isEmpty()) {
            return;
          }
          if (element.getKind().equals(METHOD)
              && enclosingMCAnno.contains(element.getSimpleName().toString())) {
            AnnotationMirror ensuresCalledMethodsAnno =
                atypeFactory.getDeclAnnotation(element, EnsuresCalledMethods.class);

            if (ensuresCalledMethodsAnno != null) {
              List<String> values =
                  AnnotationUtils.getElementValueArray(
                      ensuresCalledMethodsAnno,
                      atypeFactory.ensuresCalledMethodsValueElement,
                      String.class);
              if (values.stream()
                  .anyMatch(value -> value.contains(field.getSimpleName().toString()))) {
                List<String> methods =
                    AnnotationUtils.getElementValueArray(
                        ensuresCalledMethodsAnno,
                        atypeFactory.ensuresCalledMethodsMethodsElement,
                        String.class);
                fieldMCAnno.removeAll(methods);
              }
            }

            if (!fieldMCAnno.isEmpty()) {
              error =
                  " @EnsuresCalledMethods written on MustCall methods doesn't contain "
                      + MustCallInvokedChecker.formatMissingMustCallMethods(fieldMCAnno);
            }
          }
        }
      } else {
        error = " The enclosing element doesn't have a @MustCall annotation";
      }
    }

    if (!fieldMCAnno.isEmpty()) {
      checker.reportError(
          field,
          "required.method.not.called",
          MustCallInvokedChecker.formatMissingMustCallMethods(fieldMCAnno),
          field.asType().toString(),
          error);
    }
  }
}
