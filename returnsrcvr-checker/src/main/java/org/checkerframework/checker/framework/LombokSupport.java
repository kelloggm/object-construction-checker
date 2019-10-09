package org.checkerframework.checker.framework;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.checkerframework.checker.returnsrcvr.ReturnsRcvrAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TypesUtils;

/**
 * 
 * Lombok support for returns receiver checker
 *
 */
public class LombokSupport implements FrameworkSupport {
	
    public boolean knownToReturnThis(AnnotatedTypeMirror.AnnotatedExecutableType t, ReturnsRcvrAnnotatedTypeFactory typeFactory) {
    	ExecutableElement element = t.getElement();

        Element enclosingElement = element.getEnclosingElement();

        boolean inLombokBuilder =
            (typeFactory.hasAnnotationByName(enclosingElement, "lombok.Generated")
                    || typeFactory.hasAnnotationByName(element, "lombok.Generated"))
                && enclosingElement.getSimpleName().toString().endsWith("Builder");
        
        if (inLombokBuilder) {
            AnnotatedTypeMirror returnType = t.getReturnType();
            return returnType != null
                && enclosingElement.equals(TypesUtils.getTypeElement(returnType.getUnderlyingType()));
          }
        
        return false;
    }
  }
