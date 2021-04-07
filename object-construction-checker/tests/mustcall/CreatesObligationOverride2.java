// This test checks that 1) writing a CO annotation on an overridden method that's also
// CO is permitted, and 2) that all overridden versions of a CO method are always also CO.

import org.checkerframework.checker.mustcall.qual.*;

class CreatesObligationOverride2 {

    @InheritableMustCall("a")
    static class Foo {

        @CreatesObligation
        public void b() { }

        public void a() { }
    }

    static class Bar extends Foo {

        @Override
        @CreatesObligation
        public void b() { }
    }

    static class Baz extends Foo {

    }

    static class Qux extends Foo {
        @Override
        public void b() { }
    }

    static void test1() {
        // :: error: required.method.not.called
        Foo foo = new Foo();
        foo.a();
        foo.b();
    }

    static void test2() {
        // :: error: required.method.not.called
        Foo foo = new Bar();
        foo.a();
        foo.b();
    }

    static void test3() {
        // :: error: required.method.not.called
        Foo foo = new Baz();
        foo.a();
        foo.b();
    }

    static void test4() {
        // :: error: required.method.not.called
        Foo foo = new Qux();
        foo.a();
        foo.b();
    }

    static void test5() {
        // :: error: required.method.not.called
        Bar foo = new Bar();
        foo.a();
        foo.b();
    }

    static void test6() {
        // :: error: required.method.not.called
        Baz foo = new Baz();
        foo.a();
        foo.b();
    }

    static void test7() {
        // :: error: required.method.not.called
        Qux foo = new Qux();
        foo.a();
        foo.b();
    }
}
