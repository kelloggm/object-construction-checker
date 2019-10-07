package org.checkerframework.checker.returnsrcvr;

import com.google.auto.value.AutoValue;
import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.returnsrcvr.qual.MaybeThis;
import org.checkerframework.checker.returnsrcvr.qual.This;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

public class ReturnsRcvrAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  AnnotationMirror THIS_ANNOT;

  public ReturnsRcvrAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    THIS_ANNOT = AnnotationBuilder.fromClass(elements, This.class);
    // we have to call this explicitly
    this.postInit();
  }

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(super.createTypeAnnotator(), new ReturnsRcvrTypeAnnotator(this));
  }

  private class ReturnsRcvrTypeAnnotator extends TypeAnnotator {

    public ReturnsRcvrTypeAnnotator(AnnotatedTypeFactory typeFactory) {
      super(typeFactory);
    }

    @Override
    public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {

      AnnotatedTypeMirror returnType = t.getReturnType();
      AnnotationMirror maybeThisAnnot = AnnotationBuilder.fromClass(elements, MaybeThis.class);
      AnnotationMirror retAnnotation = returnType.getAnnotationInHierarchy(maybeThisAnnot);
      if (retAnnotation != null && AnnotationUtils.areSame(retAnnotation, THIS_ANNOT)) {
        // add @This to the receiver type
        AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = t.getReceiverType();
        receiverType.replaceAnnotation(THIS_ANNOT);
      }

      if (isBuilderSetter(t)) {
        returnType.replaceAnnotation(THIS_ANNOT);
        AnnotatedTypeMirror.AnnotatedDeclaredType receiverType = t.getReceiverType();
        receiverType.replaceAnnotation(THIS_ANNOT);
      }

      return super.visitExecutable(t, p);
    }

    /**
     * Checks if the given element is a setter in either an AutoValue builder or a Lombok builder
     */
    private boolean isBuilderSetter(AnnotatedTypeMirror.AnnotatedExecutableType t) {
      ExecutableElement element = t.getElement();

      // skip constructors
      if (element.getKind().equals(ElementKind.CONSTRUCTOR)) {
        return false;
      }

      Element enclosingElement = element.getEnclosingElement();

      boolean inAutoValueBuilder = hasAnnotation(enclosingElement, AutoValue.Builder.class);
      boolean inLombokBuilder =
          (hasAnnotationByName(enclosingElement, "lombok.Generated")
                  || hasAnnotationByName(element, "lombok.Generated"))
              && enclosingElement.getSimpleName().toString().endsWith("Builder");

      if (!inAutoValueBuilder && !inLombokBuilder) {
        // see if superclass is an AutoValue Builder, to handle generated code
        TypeMirror superclass = ((TypeElement) enclosingElement).getSuperclass();
        // if enclosingType is an interface, the superclass has TypeKind NONE
        if (!superclass.getKind().equals(TypeKind.NONE)) {
          // update enclosingElement to be for the superclass for this case
          enclosingElement = TypesUtils.getTypeElement(superclass);
          inAutoValueBuilder = enclosingElement.getAnnotation(AutoValue.Builder.class) != null;
        }
      }

      if (inAutoValueBuilder || inLombokBuilder) {
        AnnotatedTypeMirror returnType = t.getReturnType();
        return returnType != null
            && enclosingElement.equals(TypesUtils.getTypeElement(returnType.getUnderlyingType()));
      }

      return false;
    }
  }

  private boolean hasAnnotation(Element element, Class<? extends Annotation> annotClass) {
    return elements.getAllAnnotationMirrors(element).stream()
        .anyMatch(anm -> AnnotationUtils.areSameByClass(anm, annotClass));
  }

  private boolean hasAnnotationByName(Element element, String annotClassName) {
    return elements.getAllAnnotationMirrors(element).stream()
        .anyMatch(anm -> AnnotationUtils.areSameByName(anm, annotClassName));
  }
}
