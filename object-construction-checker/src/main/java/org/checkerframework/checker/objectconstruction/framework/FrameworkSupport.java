package org.checkerframework.checker.objectconstruction.framework;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

public interface FrameworkSupport {
	
	public void handleToBulder(AnnotatedTypeMirror.AnnotatedExecutableType t);
	
	public void handleBuilderBuildMethod(AnnotatedTypeMirror.AnnotatedExecutableType t);

}
