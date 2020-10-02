import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class ACOwning {

    @AlwaysCall("a")
    static class Foo {
        void a() {}
    }

    Foo makeFoo() {
        return new Foo();
    }

    static void takeOwnership(@Owning Foo foo, Foo f) {
        foo.a();
    }

    static void noOwnership(Foo foo) {}

    // :: error: missing.alwayscall
    static void takeOwnershipWrong(@Owning Foo foo) {

    }

    static void ownershipInCallee() {
        Foo f = new Foo();
        takeOwnership(f, new Foo());
        // :: error: missing.alwayscall
        Foo g = new Foo();
        noOwnership(g);
    }

    // make sure enum doesn't crash things
    static enum TestEnum {
        CASE1, CASE2, CASE3
    }


    @Owning public Foo owningAtReturn() {
        return new Foo();
    }


    void owningAtReturnTest() {
        // :: error: missing.alwayscall
        Foo f = owningAtReturn();
    }


    void ownershipTest(){
        takeOwnership(new Foo(), makeFoo());
    }
}
