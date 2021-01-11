// A simple test that @ResetMustCall works as intended wrt the Object Construction Checker.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

@MustCall("a")
class ResetMustCallSimple {

    @ResetMustCall
    void reset() { }

    @ResetMustCall("this")
    void resetThis() { }

    void a() { }

    static @MustCall({}) ResetMustCallSimple makeNoMC() {
        return null;
    }

    static void test1() {
        // :: error: required.method.not.called
        ResetMustCallSimple rmcs = makeNoMC();
        @MustCall({}) ResetMustCallSimple a = rmcs;
        rmcs.reset();
        // :: error: assignment.type.incompatible
        @CalledMethods({"reset"}) ResetMustCallSimple b = rmcs;
        @CalledMethods({}) ResetMustCallSimple c = rmcs;
    }

    static void test2() {
        // :: error: required.method.not.called
        ResetMustCallSimple rmcs = makeNoMC();
        @MustCall({}) ResetMustCallSimple a = rmcs;
        rmcs.resetThis();
        // :: error: assignment.type.incompatible
        @CalledMethods({"resetThis"}) ResetMustCallSimple b = rmcs;
        @CalledMethods({}) ResetMustCallSimple c = rmcs;
    }

    static void test3() {
        // :: error: required.method.not.called
        ResetMustCallSimple rmcs = new ResetMustCallSimple();
        rmcs.a();
        rmcs.resetThis();
    }

    static void test4() {
        ResetMustCallSimple rmcs = new ResetMustCallSimple();
        rmcs.a();
        rmcs.resetThis();
        rmcs.a();
    }

    static void test5() {
        ResetMustCallSimple rmcs = new ResetMustCallSimple();
        rmcs.resetThis();
        rmcs.a();
    }

    static void test6(boolean b) {
        ResetMustCallSimple rmcs = new ResetMustCallSimple();
        if (b) {
            rmcs.resetThis();
        }
        rmcs.a();
    }

    static void test7(boolean b) {
        // :: required.method.not.called
        ResetMustCallSimple rmcs = new ResetMustCallSimple();
        rmcs.a();
        if (b) {
            rmcs.resetThis();
        }
    }
}
