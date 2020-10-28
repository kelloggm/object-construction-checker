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
        private final @Owning Foo foo;
        private final Foo notOwningFoo;
        public FooField() {
            this.foo = new Foo();
            // :: error: missing.alwayscall
            this.notOwningFoo = new Foo();
        }

        @EnsuresCalledMethods(value = "this.foo", methods = "a")
        void b() {
            this.foo.a();
        }

    }

    void testField() {
        FooField fooField = new FooField();
        fooField.b();
    }
}
