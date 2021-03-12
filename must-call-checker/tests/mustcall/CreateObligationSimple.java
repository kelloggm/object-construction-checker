// A simple test that @CreateObligation works as intended wrt the Must Call Checker.

import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a")
class CreateObligationSimple {

    @CreateObligation
    void reset() { }

    @CreateObligation("this")
    void resetThis() { }

    static @MustCall({}) CreateObligationSimple makeNoMC() {
        return null;
    }

    static void test1() {
        CreateObligationSimple cos = makeNoMC();
        @MustCall({}) CreateObligationSimple a = cos;
        cos.reset();
        // :: error: assignment.type.incompatible
        @MustCall({}) CreateObligationSimple b = cos;
        @MustCall("a") CreateObligationSimple c = cos;
    }

    static void test2() {
        CreateObligationSimple cos = makeNoMC();
        @MustCall({}) CreateObligationSimple a = cos;
        cos.resetThis();
        // :: error: assignment.type.incompatible
        @MustCall({}) CreateObligationSimple b = cos;
        @MustCall("a") CreateObligationSimple c = cos;
    }

    static void test3() {
        Object cos = makeNoMC();
        @MustCall({}) Object a = cos;
        // :: error: mustcall.not.parseable
        ((CreateObligationSimple) cos).reset();
        // It would be better to issue an assignment incompatible error here, but the
        // error above is okay too.
        @MustCall({}) Object b = cos;
        @MustCall("a") Object c = cos;
    }

    // Rewrite of test3 that follows the instructions in the error message.
    static void test4() {
        Object cos = makeNoMC();
        @MustCall({}) Object a = cos;
        CreateObligationSimple r = ((CreateObligationSimple) cos);
        r.reset();
        // :: error: assignment.type.incompatible
        @MustCall({}) Object b = r;
        @MustCall("a") Object c = r;
    }
}
