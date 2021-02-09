// A simple test that @ResetMustCall works as intended wrt the Must Call Checker.

import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a")
class ResetMustCallSimple {

    @ResetMustCall
    void reset() { }

    @ResetMustCall("this")
    void resetThis() { }

    static @MustCall({}) ResetMustCallSimple makeNoMC() {
        return null;
    }

    static void test1() {
        ResetMustCallSimple rmcs = makeNoMC();
        @MustCall({}) ResetMustCallSimple a = rmcs;
        rmcs.reset();
        // :: error: assignment.type.incompatible
        @MustCall({}) ResetMustCallSimple b = rmcs;
        @MustCall("a") ResetMustCallSimple c = rmcs;
    }

    static void test2() {
        ResetMustCallSimple rmcs = makeNoMC();
        @MustCall({}) ResetMustCallSimple a = rmcs;
        rmcs.resetThis();
        // :: error: assignment.type.incompatible
        @MustCall({}) ResetMustCallSimple b = rmcs;
        @MustCall("a") ResetMustCallSimple c = rmcs;
    }

    static void test3() {
        Object rmcs = makeNoMC();
        @MustCall({}) Object a = rmcs;
        // :: error: mustcall.not.parseable
        ((ResetMustCallSimple) rmcs).reset();
        // It would be better to issue an assignment incompatible error here, but the
        // error above is okay too.
        @MustCall({}) Object b = rmcs;
        @MustCall("a") Object c = rmcs;
    }

    // Rewrite of test3 that follows the instructions in the error message.
    static void test4() {
        Object rmcs = makeNoMC();
        @MustCall({}) Object a = rmcs;
        ResetMustCallSimple r = ((ResetMustCallSimple) rmcs);
        r.reset();
        // :: error: assignment.type.incompatible
        @MustCall({}) Object b = r;
        @MustCall("a") Object c = r;
    }
}
