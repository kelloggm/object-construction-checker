import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class ACOwning {

    @AlwaysCall("a")
    static class Foo {
        void a() {}
    }

    static void takeOwnership(@Owning Foo foo) {
        foo.a();
    }

    static void noOwnership(Foo foo) {}

    // :: error: missing.alwayscall
    static void takeOwnershipWrong(@Owning Foo foo) {

    }

    static void ownershipInCallee() {
        Foo f = new Foo();
        takeOwnership(f);
        // :: error: missing.alwayscall
        Foo g = new Foo();
        noOwnership(g);
    }

}
