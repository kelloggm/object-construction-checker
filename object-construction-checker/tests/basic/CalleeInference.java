import org.checkerframework.checker.objectconstruction.qual.*;

/* The simplest inference test case Martin could think of */
class CalleeInference {
    void build(@CalledMethods({"a", "b", "c"}) CalleeInference this) { }

    void a() { }

    void b() {}

    void c() {}

    static void callB(CalleeInference x) {
        x.b();
    }

    static void allInOneMethod() {
        CalleeInference y = new CalleeInference();
        y.a();
        y.b();
        y.c();
        y.build();
    }

    static void calleeFirst() {
        CalleeInference y = new CalleeInference();
        y.a();
        callB(y);
        y.c();
        y.build();
    }

    static void calleeSecond() {
        CalleeInference y = new CalleeInference();
        y.a();
        y.c();
        callB(y);
        y.build();
    }
}