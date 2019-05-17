package org.checkerframework.checker.builder.lombok;

import com.sun.source.tree.Tree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/** This visitor intentionally does not check anything. */
public class LombokBuilderVisitor extends BaseTypeVisitor<LombokBuilderAnnotatedTypeFactory> {
  /** To permit reflective loading with a BaseTypeChecker */
  public LombokBuilderVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void scan(Tree tree, Void p) {
    return null;
  }
}
