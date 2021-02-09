// A test that methods containing calls to other @ResetMustCall methods work as intended.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

@MustCall("a")
class ResetMustCallIndirect {

    @ResetMustCall
    void reset() { }

    void a() { }

    static @MustCall({}) ResetMustCallSimple makeNoMC() {
        return null;
    }

    public static void resetIndirect_no_anno(ResetMustCallIndirect r) {
        // :: error: reset.not.owning
        r.reset();
    }

    @ResetMustCall("#1")
    public static void resetIndirect_anno(ResetMustCallIndirect r) {
        r.reset();
    }

    public static void reset_local() {
        // :: error: required.method.not.called
        ResetMustCallIndirect r = new ResetMustCallIndirect();
        r.reset();
    }

    public static void reset_local2() {
        ResetMustCallIndirect r = new ResetMustCallIndirect();
        r.reset();
        r.a();
    }

    public static void reset_local3() {
        // :: error: required.method.not.called
        ResetMustCallIndirect r = new ResetMustCallIndirect();
        // :: error: mustcall.not.parseable
        ((ResetMustCallIndirect) r).reset();
    }

    // :: error: required.method.not.called
    public static void test(@Owning ResetMustCallIndirect r) {
        resetIndirect_anno(r);
    }

    public static void test2(ResetMustCallIndirect r) {
        // :: error: reset.not.owning
        resetIndirect_anno(r);
    }

    public static void test3(@Owning ResetMustCallIndirect r) {
        resetIndirect_anno(r);
        r.a();
    }
}
