package org.checkerframework.checker.builder.autovalue;

import com.sun.source.tree.Tree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/** This visitor intentionally does not check anything. */
public class AutoValueBuilderVisitor extends BaseTypeVisitor<AutoValueBuilderAnnotatedTypeFactory> {
  /** To permit reflective loading with a BaseTypeChecker */
  public AutoValueBuilderVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void scan(Tree tree, Void p) {
    return null;
  }
}
