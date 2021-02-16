// A simpler test that @ResetMustCall works as intended wrt the Object Construction Checker.

// This test has been modified to expect that ResetMustCall is feature-flagged to off.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

@MustCall("a")
class ResetMustCallSimpler {

    @ResetMustCall
    void reset() { }

    @ResetMustCall("this")
    void resetThis() { }

    void a() { }

    static @MustCall({}) ResetMustCallSimpler makeNoMC() {
        // :: error: return.type.incompatible
        return new ResetMustCallSimpler();
    }

    static void test1() {
        ResetMustCallSimpler rmcs = makeNoMC();
        @MustCall({}) ResetMustCallSimpler a = rmcs;
        rmcs.reset();
        @CalledMethods({"reset"}) ResetMustCallSimpler b = rmcs;
        @CalledMethods({}) ResetMustCallSimpler c = rmcs;
    }
}
