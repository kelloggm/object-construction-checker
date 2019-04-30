import org.checkerframework.checker.builder.qual.*;

/* Simple inference of a fluent builder */
class SimpleFluentInference {
    SimpleFluentInference build(@CalledMethods({"a", "b"}) SimpleFluentInference this) { return this; }
    SimpleFluentInference weakbuild(@CalledMethods({"a"}) SimpleFluentInference this) { return this; }

    @ReturnsReceiver
    SimpleFluentInference a() { return this; }

    @ReturnsReceiver
    SimpleFluentInference b() { return this; }

    // intentionally does not have an @ReturnsReceiver annotation
    SimpleFluentInference c() { return new SimpleFluentInference(); }

    static void doStuffCorrect() {
        SimpleFluentInference s = new SimpleFluentInference()
                .a()
                .b()
                .build();
    }

    static void doStuffWrong() {
        SimpleFluentInference s = new SimpleFluentInference()
                .a()
                // :: error: method.invocation.invalid
                .build();
    }

    static void doStuffRightWeak() {
        SimpleFluentInference s = new SimpleFluentInference()
                .a()
                .weakbuild();
    }

    static void noReturnsReceiverAnno() {
        SimpleFluentInference s = new SimpleFluentInference()
                .a()
                .b()
                .c()
                // :: error: method.invocation.invalid
                .build();
    }
}