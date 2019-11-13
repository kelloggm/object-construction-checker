package org.checkerframework.checker.objectconstruction.framework;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.checker.objectconstruction.ObjectConstructionAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.VariableTree;

public class LombokSupport implements FrameworkSupport{
	
	private ObjectConstructionAnnotatedTypeFactory atypeFactory;
	
	public LombokSupport (ObjectConstructionAnnotatedTypeFactory atypeFactory) {
		this.atypeFactory = atypeFactory;
	}

	// The list is copied from lombok.core.handlers.HandlerUtil. The list cannot be used from that
	// class directly because Lombok does not provide class files for its own implementation, to
	// prevent itself from being accidentally added to clients' compile classpaths. This design
	// decision means that it is impossible to depend directly on Lombok internals.
	/** The list of annotations that Lombok treats as non-null. */
	public static final List<String> NONNULL_ANNOTATIONS =
	    Collections.unmodifiableList(
	        Arrays.asList(
	            "android.annotation.NonNull",
	            "android.support.annotation.NonNull",
	            "com.sun.istack.internal.NotNull",
	            "edu.umd.cs.findbugs.annotations.NonNull",
	            "javax.annotation.Nonnull",
	            // "javax.validation.constraints.NotNull", // The field might contain a null value
	            // until it is persisted.
	            "lombok.NonNull",
	            "org.checkerframework.checker.nullness.qual.NonNull",
	            "org.eclipse.jdt.annotation.NonNull",
	            "org.eclipse.jgit.annotations.NonNull",
	            "org.jetbrains.annotations.NotNull",
	            "org.jmlspecs.annotation.NonNull",
	            "org.netbeans.api.annotations.common.NonNull",
	            "org.springframework.lang.NonNull"));

	  
	@Override
	public void handleToBulder(AnnotatedExecutableType t) {
		
		AnnotatedTypeMirror returnType = t.getReturnType();
		ExecutableElement element = t.getElement();

	    TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
	    
		if (FrameworkSupportUtils.hasAnnotation(element, "lombok.Generated")
	            || FrameworkSupportUtils.hasAnnotation(enclosingElement, "lombok.Generated")) {
			handleToBuilderType(returnType, returnType.getUnderlyingType(), enclosingElement);
		}
		
	}
	
	private void handleToBuilderType(
		      AnnotatedTypeMirror type, TypeMirror builderType, Element classElement) {
		System.out.println("start handle to builder type");
		Element builderElement = TypesUtils.getTypeElement(builderType);
		List<String> requiredProperties = getLombokRequiredProperties(builderElement);

		System.out.println(requiredProperties.size());
		AnnotationMirror calledMethodsAnno =
				atypeFactory.createCalledMethods(requiredProperties.toArray(new String[0]));
		type.replaceAnnotation(calledMethodsAnno);
	}
	
	/**
	   * computes the required properties of a @lombok.Builder class, i.e., the names of the fields
	   * with @lombok.NonNull annotations
	   *
	   * @param lombokClassElement the class with the @lombok.Builder annotation
	   * @return a list of required property names
	   */
	  private List<String> getLombokRequiredProperties(final Element lombokClassElement) {
	    List<String> requiredPropertyNames = new ArrayList<>();
	    List<String> defaultedPropertyNames = new ArrayList<>();
	    for (Element member : lombokClassElement.getEnclosedElements()) {
	      if (member.getKind() == ElementKind.FIELD) {
	        for (AnnotationMirror anm : atypeFactory.getElementUtils().getAllAnnotationMirrors(member)) {
	          if (NONNULL_ANNOTATIONS.contains(AnnotationUtils.annotationName(anm))) {
	            requiredPropertyNames.add(member.getSimpleName().toString());
	          }
	        }
	      } else if (member.getKind() == ElementKind.METHOD
	          && FrameworkSupportUtils.hasAnnotation(member, "lombok.Generated")) {
	        String methodName = member.getSimpleName().toString();
	        // Handle fields with @Builder.Default annotations.
	        // If a field foo has an @Builder.Default annotation, Lombok always generates a method
	        // called $default$foo.
	        if (methodName.startsWith("$default$")) {
	          String propName = methodName.substring(9); // $default$ has 9 characters
	          defaultedPropertyNames.add(propName);
	        }
	      } else if (member.getKind().isClass() && member.toString().endsWith("Builder")) {
	        // If a field bar has an @Singular annotation, Lombok always generates a method called
	        // clearBar in the builder class itself. Therefore, search the builder for such a method,
	        // and extract the appropriate property name to treat as defaulted.
	        for (Element builderMember : member.getEnclosedElements()) {
	          if (builderMember.getKind() == ElementKind.METHOD
	              && FrameworkSupportUtils.hasAnnotation(builderMember, "lombok.Generated")) {
	            String methodName = builderMember.getSimpleName().toString();
	            if (methodName.startsWith("clear")) {
	              String propName =
	                  Introspector.decapitalize(methodName.substring(5)); // clear has 5 characters
	              defaultedPropertyNames.add(propName);
	            }
	          } else if (builderMember.getKind() == ElementKind.FIELD) {
	            VariableTree variableTree = (VariableTree) atypeFactory.declarationFromElement(builderMember);
	            if (variableTree != null && variableTree.getInitializer() != null) {
	              String propName = variableTree.getName().toString();
	              defaultedPropertyNames.add(propName);
	              atypeFactory.getDefaultedElements().put(builderMember, propName);
	            } else if (atypeFactory.getDefaultedElements().containsKey(builderMember)) {
	              defaultedPropertyNames.add(atypeFactory.getDefaultedElements().get(builderMember));
	            }
	          }
	        }
	      }
	    }
	    requiredPropertyNames.removeAll(defaultedPropertyNames);
	    return requiredPropertyNames;
	  }
	  
	  

}
