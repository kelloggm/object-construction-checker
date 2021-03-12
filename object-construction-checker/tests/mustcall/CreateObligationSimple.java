// A simple test that @CreateObligation works as intended wrt the Object Construction Checker.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

@MustCall("a")
class CreateObligationSimple {

    @CreateObligation
    void reset() { }

    @CreateObligation("this")
    void resetThis() { }

    void a() { }

    static @MustCall({}) CreateObligationSimple makeNoMC() {
        return null;
    }

    static void test1() {
        // :: error: required.method.not.called
        CreateObligationSimple cos = makeNoMC();
        @MustCall({}) CreateObligationSimple a = cos;
        cos.reset();
        // :: error: assignment.type.incompatible
        @CalledMethods({"reset"}) CreateObligationSimple b = cos;
        @CalledMethods({}) CreateObligationSimple c = cos;
    }

    static void test2() {
        // :: error: required.method.not.called
        CreateObligationSimple cos = makeNoMC();
        @MustCall({}) CreateObligationSimple a = cos;
        cos.resetThis();
        // :: error: assignment.type.incompatible
        @CalledMethods({"resetThis"}) CreateObligationSimple b = cos;
        @CalledMethods({}) CreateObligationSimple c = cos;
    }

    static void test3() {
        // :: error: required.method.not.called
        CreateObligationSimple cos = new CreateObligationSimple();
        cos.a();
        cos.resetThis();
    }

    static void test4() {
        CreateObligationSimple cos = new CreateObligationSimple();
        cos.a();
        cos.resetThis();
        cos.a();
    }

    static void test5() {
        CreateObligationSimple cos = new CreateObligationSimple();
        cos.resetThis();
        cos.a();
    }

    static void test6(boolean b) {
        CreateObligationSimple cos = new CreateObligationSimple();
        if (b) {
            cos.resetThis();
        }
        cos.a();
    }

    static void test7(boolean b) {
        // :: error: required.method.not.called
        CreateObligationSimple cos = new CreateObligationSimple();
        cos.a();
        if (b) {
            cos.resetThis();
        }
    }
}
