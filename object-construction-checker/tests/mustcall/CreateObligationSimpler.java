// A simpler test that @CreateObligation works as intended wrt the Object Construction Checker.

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
        return null;
    }

    static void test1() {
        // :: error: required.method.not.called
        CreateObligationSimpler cos = makeNoMC();
        @MustCall({}) CreateObligationSimpler a = cos;
        cos.reset();
        // :: error: assignment.type.incompatible
        @CalledMethods({"reset"}) CreateObligationSimpler b = cos;
        @CalledMethods({}) CreateObligationSimpler c = cos;
    }
}
