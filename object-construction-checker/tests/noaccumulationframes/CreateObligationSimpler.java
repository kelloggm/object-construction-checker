// A simpler test that @CreateObligation works as intended wrt the Object Construction Checker.

// This test has been modified to expect that CreateObligation is feature-flagged to off.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

@MustCall("a")
class CreateObligationSimpler {

    @CreateObligation
    void reset() { }

    @CreateObligation("this")
    void resetThis() { }

    void a() { }

    static @MustCall({}) CreateObligationSimpler makeNoMC() {
        // :: error: return.type.incompatible
        return new CreateObligationSimpler();
    }

    static void test1() {
        CreateObligationSimpler cos = makeNoMC();
        @MustCall({}) CreateObligationSimpler a = cos;
        cos.reset();
        @CalledMethods({"reset"}) CreateObligationSimpler b = cos;
        @CalledMethods({}) CreateObligationSimpler c = cos;
    }
}
