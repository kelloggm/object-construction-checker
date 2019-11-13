package org.checkerframework.checker.objectconstruction.framework;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import com.sun.source.tree.NewClassTree;

public interface FrameworkSupport {
	
	public void handleToBulder(AnnotatedTypeMirror.AnnotatedExecutableType t);
	
	public void handleBuilderBuildMethod(AnnotatedTypeMirror.AnnotatedExecutableType t);
	
	public void handleConstructor(NewClassTree tree, AnnotatedTypeMirror type);

}
