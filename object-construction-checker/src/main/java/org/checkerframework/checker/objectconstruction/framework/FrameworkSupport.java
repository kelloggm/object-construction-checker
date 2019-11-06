package org.checkerframework.checker.objectconstruction.framework;

import javax.lang.model.element.Element;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

public interface FrameworkSupport {
	
	public void handleToBulder(AnnotatedTypeMirror.AnnotatedExecutableType t, Element classElement);

}
