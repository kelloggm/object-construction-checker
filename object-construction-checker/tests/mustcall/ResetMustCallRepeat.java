// A simple test that @ResetMustCall is repeatable and works as intended.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

@MustCall("a")
class ResetMustCallRepeat {

    @ResetMustCall("this")
    @ResetMustCall("#1")
    void reset(ResetMustCallRepeat r) { }

    void a() { }

    static @MustCall({}) ResetMustCallRepeat makeNoMC() {
        return null;
    }

    static void test1() {
        // :: error: required.method.not.called
        ResetMustCallRepeat rmcs1 = makeNoMC();
        // :: error: required.method.not.called
        ResetMustCallRepeat rmcs2 = makeNoMC();
        @MustCall({}) ResetMustCallRepeat a = rmcs2;
        @MustCall({}) ResetMustCallRepeat a2 = rmcs2;
        rmcs2.a();
        rmcs1.reset(rmcs2);
        // :: error: assignment.type.incompatible
        @CalledMethods({"reset"}) ResetMustCallRepeat b = rmcs1;
        @CalledMethods({}) ResetMustCallRepeat c = rmcs1;
        @CalledMethods({}) ResetMustCallRepeat d = rmcs2;
        // :: error: assignment.type.incompatible
        @CalledMethods({"a"}) ResetMustCallRepeat e = rmcs2;
    }

    static void test3() {
        // :: error: required.method.not.called
        ResetMustCallRepeat rmcs = new ResetMustCallRepeat();
        // :: error: required.method.not.called
        ResetMustCallRepeat rmcs2 = new ResetMustCallRepeat();
        rmcs.a();
        rmcs.reset(rmcs2);
    }

    static void test4() {
        ResetMustCallRepeat rmcs = new ResetMustCallRepeat();
        // :: error: required.method.not.called
        ResetMustCallRepeat rmcs2 = new ResetMustCallRepeat();
        rmcs.a();
        rmcs.reset(rmcs2);
        rmcs.a();
    }

    static void test5() {
        // :: error: required.method.not.called
        ResetMustCallRepeat rmcs = new ResetMustCallRepeat();
        ResetMustCallRepeat rmcs2 = new ResetMustCallRepeat();
        rmcs.a();
        rmcs.reset(rmcs2);
        rmcs2.a();
    }

    static void test6() {
        ResetMustCallRepeat rmcs = new ResetMustCallRepeat();
        ResetMustCallRepeat rmcs2 = new ResetMustCallRepeat();
        rmcs.a();
        rmcs.reset(rmcs2);
        rmcs2.a();
        rmcs.a();
    }
}
