import org.checkerframework.checker.builder.qual.*;

/* The simplest inference test case Martin could think of */
class SimpleInference {
    void build(@CalledMethods({"a"}) SimpleInference this) { }

    void a() { }

    static void doStuffCorrect() {
        SimpleInference s = new SimpleInference();
        s.a();
        s.build();
    }

    static void doStuffWrong() {
        SimpleInference s = new SimpleInference();
        // :: error: method.invocation.invalid
        s.build();
    }
}