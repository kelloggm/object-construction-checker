package org.checkerframework.checker.objectconstruction;

import static javax.lang.model.element.ElementKind.METHOD;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;
import org.checkerframework.checker.calledmethods.CalledMethodsVisitor;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
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
    if (AnnotationUtils.areSameByClass(anno, EnsuresCalledMethodsVarArgs.class)) {
      // we can't verify these yet.  emit an error (which will have to be suppressed) for now
      checker.report(node, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.unverified"));
      return null;
    }
    return super.visitAnnotation(node, p);
  }

  @Override
  public Void visitMethod(MethodTree node, Void p) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(node);
    AnnotationMirror annot = atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethodsVarArgs.class);
    if (annot != null) {
      if (!elt.isVarArgs()) {
        checker.report(
            node, new DiagMessage(Diagnostic.Kind.ERROR, "ensuresvarargs.annotation.invalid"));
        return null;
      }
    }
    return super.visitMethod(node, p);
  }

  @Override
  public Void visitVariable(VariableTree node, Void p) {
    Element varElement = TreeUtils.elementFromTree(node);

    if (varElement.getKind().isField()
        && atypeFactory.getDeclAnnotation(varElement, Owning.class) != null) {
      List<String> fieldMCAnno = atypeFactory.getMustCallValue(varElement);
      if (ElementUtils.isFinal(varElement)) {
        checkFinalOwningField(varElement);
      } else if (!fieldMCAnno.isEmpty()) {
        List<String> varMCValue = atypeFactory.getMustCallValue(varElement);
        checker.reportError(
            node,
            "required.method.not.called",
            MustCallInvokedChecker.formatMissingMustCallMethods(varMCValue),
            varElement.asType().toString(),
            " Non-final owning field might be overwritten");
      }
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
  private void checkFinalOwningField(Element field) {
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
                  ValueCheckerUtils.getValueOfAnnotationWithStringArgument(
                      ensuresCalledMethodsAnno);
              if (values.stream()
                  .anyMatch(value -> value.contains(field.getSimpleName().toString()))) {
                List<String> methods =
                    AnnotationUtils.getElementValueArray(
                        ensuresCalledMethodsAnno, "methods", String.class, false);
                fieldMCAnno.removeAll(methods);
              }
            }

            if (!fieldMCAnno.isEmpty()) {
              error =
                  " @EnsuresCalledMethods written on MustCall methods doesn't contain "
                      + MustCallInvokedChecker.formatMissingMustCallMethods(fieldMCAnno);
            } else {
              error =
                  " There  is no @MustCall annotation written on the enclosing method"
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
