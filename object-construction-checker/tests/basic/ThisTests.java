// A test case to make sure that refinement of various types of things whose
// toString() function in the AST should return something like "(this)."
// This test exists to prevent Martin from doing bad stuff.

import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.returnsrcvr.qual.*;

class ThisTests {

    @This ThisTests foo() { return this; }
    @This ThisTests bar() { return this; }
    @This ThisTests baz() { return this; }

    void testThisWithNoParens() {
        @CalledMethods({"foo", "bar"}) ThisTests clone = this.foo().bar();
    }

    void testThisWithNoParensFail() {
        // :: error: assignment.type.incompatible
        @CalledMethods({"foo", "bar", "baz"}) ThisTests clone = this.foo().bar();
    }

    void testThisImplicit() {
        @CalledMethods({"foo", "bar"}) ThisTests clone = foo().bar();
    }

    void testThisImplicitFail() {
        // :: error: assignment.type.incompatible
        @CalledMethods({"foo", "bar", "baz"}) ThisTests clone = foo().bar();
    }

    void testThisWithOneParens() {
        @CalledMethods({"foo", "bar"}) ThisTests clone = (this).foo().bar();
    }

    void testThisWithOneParensFail() {
        // :: error: assignment.type.incompatible
        @CalledMethods({"foo", "bar", "baz"}) ThisTests clone = (this).foo().bar();
    }

    void testThisWithTwoParens() {
        @CalledMethods({"foo", "bar"}) ThisTests clone = ((this)).foo().bar();
    }

    void testThisWithTwoParensFail() {
        // :: error: assignment.type.incompatible
        @CalledMethods({"foo", "bar", "baz"}) ThisTests clone = ((this)).foo().bar();
    }

    // Note that all of these fail because String#toString doesn't have an @This annotation.
    void testStringThis() {
        // :: error: assignment.type.incompatible
        @CalledMethods({"toString"}) String s = "this".toString();
    }

    void testStringThisFail() {
        // :: error: assignment.type.incompatible
        @CalledMethods({"toString", "clone"}) String s = "this".toString();
    }

    void testStringThisParens() {
        // :: error: assignment.type.incompatible
        @CalledMethods({"toString"}) String s = "(this)".toString();
    }

    void testStringThisParensFail() {
        // :: error: assignment.type.incompatible
        @CalledMethods({"toString", "clone"}) String s = "(this)".toString();
    }
}