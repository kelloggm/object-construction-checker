// tests that the MustCall checker respects the OCC's Owning and NotOwning annotations on return values.

import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class BorrowOnReturn {
    @MustCall("a") class Foo {
        void a() { }
    }

    @Owning Object getOwnedFoo() {
        // :: error: return.type.incompatible
        return new Foo();
    }

    Object getNoAnnoFoo() {
        // Treat as owning, so warn
        // :: error: return.type.incompatible
        return new Foo();
    }

    @NotOwning Object getNotOwningFooWrong() {
        // OCC must-call checker will warn about this; MC checker isn't responsible
        return new Foo();
    }

    Object getNotOwningFooRightButNoNotOwningAnno() {
        Foo f = new Foo();
        f.a();
        // This is still an error for now, because it's treated as an owning pointer. TODO: fix this kind of FP?
        // :: error: return.type.incompatible
        return f;
    }

    @NotOwning Object getNotOwningFooRight() {
        Foo f = new Foo();
        f.a();
        return f;
    }

    @MustCall("a") Object getNotOwningFooRight2() {
        return new Foo();
    }
}
