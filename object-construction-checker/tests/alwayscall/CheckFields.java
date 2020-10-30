import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class CheckFields {

    @AlwaysCall("a")
    static class Foo {
        void a() {}
    }

    Foo makeFoo() {
        return new Foo();
    }

    @AlwaysCall("b")
    static class FooField {
        private final @Owning Foo finalOwningFoo;
        private final Foo finalNotOwningFoo;
        private @Owning Foo owningFoo;
        private Foo notOwningFoo;
        public FooField() {
            this.finalOwningFoo = new Foo();
            // :: error: missing.alwayscall
            this.finalNotOwningFoo = new Foo();
        }

        void assingToFinalOwningField() {
            Foo f = new Foo();
            this.owningFoo = f;
        }

        void assingToFinalNotOwningField() {
            // :: error: missing.alwayscall
            Foo f = new Foo();
            this.notOwningFoo = f;
        }

        Foo getOwningFoo() {
            return this.owningFoo;
        }

        @EnsuresCalledMethods(value = {"this.finalOwningFoo", "this.owningFoo"}, methods = "a")
        void b() {
            this.finalOwningFoo.a();
            this.owningFoo.a();
        }

    }

    void testField() {
        FooField fooField = new FooField();
        fooField.b();
    }

    void testAccessField() {
        FooField fooField = new FooField();
        fooField.owningFoo = new Foo();
        fooField.b();
    }

    void testAccessFieldWrong() {
        // :: error: missing.alwayscall
        FooField fooField = new FooField();
        fooField.owningFoo = new Foo();
        // :: error: missing.alwayscall
        fooField.notOwningFoo = new Foo();

    }
}
