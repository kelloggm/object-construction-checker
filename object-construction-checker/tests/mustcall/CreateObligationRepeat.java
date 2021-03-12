// A simple test that @CreateObligation is repeatable and works as intended.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

@MustCall("a")
class CreateObligationRepeat {

    @CreateObligation("this")
    @CreateObligation("#1")
    void reset(CreateObligationRepeat r) { }

    void a() { }

    static @MustCall({}) CreateObligationRepeat makeNoMC() {
        return null;
    }

    static void test1() {
        // :: error: required.method.not.called
        CreateObligationRepeat cos1 = makeNoMC();
        // :: error: required.method.not.called
        CreateObligationRepeat cos2 = makeNoMC();
        @MustCall({}) CreateObligationRepeat a = cos2;
        @MustCall({}) CreateObligationRepeat a2 = cos2;
        cos2.a();
        cos1.reset(cos2);
        // :: error: assignment.type.incompatible
        @CalledMethods({"reset"}) CreateObligationRepeat b = cos1;
        @CalledMethods({}) CreateObligationRepeat c = cos1;
        @CalledMethods({}) CreateObligationRepeat d = cos2;
        // :: error: assignment.type.incompatible
        @CalledMethods({"a"}) CreateObligationRepeat e = cos2;
    }

    static void test3() {
        // :: error: required.method.not.called
        CreateObligationRepeat cos = new CreateObligationRepeat();
        // :: error: required.method.not.called
        CreateObligationRepeat cos2 = new CreateObligationRepeat();
        cos.a();
        cos.reset(cos2);
    }

    static void test4() {
        CreateObligationRepeat cos = new CreateObligationRepeat();
        // :: error: required.method.not.called
        CreateObligationRepeat cos2 = new CreateObligationRepeat();
        cos.a();
        cos.reset(cos2);
        cos.a();
    }

    static void test5() {
        // :: error: required.method.not.called
        CreateObligationRepeat cos = new CreateObligationRepeat();
        CreateObligationRepeat cos2 = new CreateObligationRepeat();
        cos.a();
        cos.reset(cos2);
        cos2.a();
    }

    static void test6() {
        CreateObligationRepeat cos = new CreateObligationRepeat();
        CreateObligationRepeat cos2 = new CreateObligationRepeat();
        cos.a();
        cos.reset(cos2);
        cos2.a();
        cos.a();
    }
}
