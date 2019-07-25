import org.checkerframework.checker.objectconstruction.qual.*;

class UnparseablePredicate {

    // :: error: predicate.invalid
    void unclosedOpen(@CalledMethodsPredicate("(foo AND bar") Object x) { }

    // :: error: predicate.invalid
    void unopenedClose(@CalledMethodsPredicate("foo OR bar)") Object x) { }
}