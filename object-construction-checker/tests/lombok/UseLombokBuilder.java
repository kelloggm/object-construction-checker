// tests that using a lombok builder from an external jar incorrectly
// still produces an error

import testlib.lombok.Foo;

class UseLombokBuilder {
    void test() {
        // :: error: finalizer.invocation.invalid
        Foo foo = Foo.builder().build();

        Foo foo2 = Foo.builder().requiredProperty("foo!").build();
    }
}