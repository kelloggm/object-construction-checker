// A simpler test that @ResetMustCall works as intended wrt the Object Construction Checker.

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
        return null;
    }

    static void test1() {
        // :: error: required.method.not.called
        ResetMustCallSimpler rmcs = makeNoMC();
        @MustCall({}) ResetMustCallSimpler a = rmcs;
        rmcs.reset();
        // :: error: assignment.type.incompatible
        @CalledMethods({"reset"}) ResetMustCallSimpler b = rmcs;
        @CalledMethods({}) ResetMustCallSimpler c = rmcs;
    }
}
