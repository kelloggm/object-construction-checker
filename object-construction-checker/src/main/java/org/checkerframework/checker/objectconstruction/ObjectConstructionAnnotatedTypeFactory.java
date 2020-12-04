package org.checkerframework.checker.objectconstruction;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsBottom;
import org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.unconnectedsocket.UnconnectedSocketAnnotatedTypeFactory;
import org.checkerframework.checker.unconnectedsocket.UnconnectedSocketChecker;
import org.checkerframework.checker.unconnectedsocket.qual.Unconnected;
import org.checkerframework.checker.mustcall.qual.MustCallChoice;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The annotated type factory for the object construction checker. Primarily responsible for the
 * subtyping rules between @CalledMethod annotations.
 */
public class ObjectConstructionAnnotatedTypeFactory extends CalledMethodsAnnotatedTypeFactory {

  /**
   * Default constructor matching super. Should be called automatically.
   *
   * @param checker the checker associated with this type factory
   */
  public ObjectConstructionAnnotatedTypeFactory(final BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return getBundledTypeQualifiers(
        CalledMethods.class, CalledMethodsBottom.class, CalledMethodsPredicate.class);
  }

  /**
   * Creates a @CalledMethods annotation whose values are the given strings.
   *
   * @param val the methods that have been called
   * @return an annotation indicating that the given methods have been called
   */
  public AnnotationMirror createCalledMethods(final String... val) {
    return createAccumulatorAnnotation(Arrays.asList(val));
  }

  @Override
  public void postAnalyze(ControlFlowGraph cfg) {
    if (checker.hasOption(ObjectConstructionChecker.CHECK_MUST_CALL)) {
      MustCallInvokedChecker mustCallInvokedChecker =
          new MustCallInvokedChecker(this, this.checker, this.analysis);
      mustCallInvokedChecker.checkMustCallInvoked(cfg);
    }
    super.postAnalyze(cfg);
  }

  /**
   * Returns the String value of @MustCall annotation declared on the class type of {@code tree}.
   */
  List<String> getMustCallValue(Tree tree) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotationMirror mustCallAnnotation =
        mustCallAnnotatedTypeFactory.getAnnotatedType(tree).getAnnotation(MustCall.class);

    return getMustCallValues(mustCallAnnotation);
  }

  /**
   * Returns the String value of @MustCall annotation declared on the class type of {@code element}.
   */
  List<String> getMustCallValue(Element element) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotatedTypeMirror mustCallAnnotatedType =
        mustCallAnnotatedTypeFactory.getAnnotatedType(element);
    AnnotationMirror mustCallAnnotation = mustCallAnnotatedType.getAnnotation(MustCall.class);

    return getMustCallValues(mustCallAnnotation);
  }

  private List<String> getMustCallValues(AnnotationMirror mustCallAnnotation) {
    List<String> mustCallValues =
        (mustCallAnnotation != null)
            ? ValueCheckerUtils.getValueOfAnnotationWithStringArgument(mustCallAnnotation)
            : Collections.emptyList();
    return mustCallValues;
  }

  boolean hasMustCall(Tree t) {
    return !getMustCallValue(t).isEmpty();
  }

  /**
   * Returns true iff the unconnected sockets checker determined that this tree represents a socket
   * that is definitely unconnected.
   */
  public boolean isUnconnectedSocket(Tree tree) {
    UnconnectedSocketAnnotatedTypeFactory usatf =
            getTypeFactoryOfSubchecker(UnconnectedSocketChecker.class);
    AnnotatedTypeMirror usatm = usatf.getAnnotatedType(tree);
    return usatm.hasAnnotation(Unconnected.class);
  }

  boolean hasMustCallChoice(Tree tree) {
    Element elt = TreeUtils.elementFromTree(tree);
    return hasMustCallChoice(elt);
  }

  boolean hasMustCallChoice(Element elt) {
    MustCallAnnotatedTypeFactory mustCallAnnotatedTypeFactory =
        getTypeFactoryOfSubchecker(MustCallChecker.class);
    return mustCallAnnotatedTypeFactory.getDeclAnnotationNoAliases(elt, MustCallChoice.class)
        != null;
  }
}
