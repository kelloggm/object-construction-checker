// A test that methods containing calls to other @CreateObligation methods work as intended.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

@MustCall("a")
class CreateObligationIndirect {

    @CreateObligation
    void reset() { }

    void a() { }

    static @MustCall({}) CreateObligationSimple makeNoMC() {
        return null;
    }

    public static void resetIndirect_no_anno(CreateObligationIndirect r) {
        // :: error: reset.not.owning
        r.reset();
    }

    @CreateObligation("#1")
    public static void resetIndirect_anno(CreateObligationIndirect r) {
        r.reset();
    }

    public static void reset_local() {
        // :: error: required.method.not.called
        CreateObligationIndirect r = new CreateObligationIndirect();
        r.reset();
    }

    public static void reset_local2() {
        CreateObligationIndirect r = new CreateObligationIndirect();
        r.reset();
        r.a();
    }

    public static void reset_local3() {
        // :: error: required.method.not.called
        CreateObligationIndirect r = new CreateObligationIndirect();
        // :: error: mustcall.not.parseable :: error: reset.not.owning
        ((CreateObligationIndirect) r).reset();
    }

    // :: error: required.method.not.called
    public static void test(@Owning CreateObligationIndirect r) {
        resetIndirect_anno(r);
    }

    public static void test2(CreateObligationIndirect r) {
        // :: error: reset.not.owning
        resetIndirect_anno(r);
    }

    public static void test3(@Owning CreateObligationIndirect r) {
        resetIndirect_anno(r);
        r.a();
    }
}
