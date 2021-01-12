// A simple test that @ResetMustCall works as intended wrt the Must Call Checker.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;

@MustCall("a")
class ResetMustCallVerySimple {

    @ResetMustCall
    void reset() {
    }

    static @MustCall({})
    ResetMustCallVerySimple makeNoMC() {
        return null;
    }

    static void test1() {
        // :: error: required.method.not.called
        ResetMustCallVerySimple rmcs = makeNoMC();
        @MustCall({}) ResetMustCallVerySimple a = rmcs;
        rmcs.reset();
        // :: error: assignment.type.incompatible
        @CalledMethods({"reset"}) ResetMustCallVerySimple b = rmcs;
        @CalledMethods({}) ResetMustCallVerySimple c = rmcs;
    }
}