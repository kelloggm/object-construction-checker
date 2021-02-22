// A test for a bad interaction between RMC and subtyping
// that could happen if RMC was unsound.

import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class RMCInSubtype {
    static class Foo {

        @ResetMustCall("this")
        void resetFoo() { }
    }

    @MustCall("a")
    static class Bar extends Foo {
        void a() { }
    }

    static void test() {
        // :: error: required.method.not.called
        @MustCall("a") Foo f = new Bar();
        f.resetFoo();
    }
}
