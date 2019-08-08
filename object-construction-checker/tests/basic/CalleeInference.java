import org.checkerframework.checker.objectconstruction.qual.*;

/* The simplest inference test case Martin could think of */
class CalleeInference {
    void build(@CalledMethods({"a", "b", "c"}) CalleeInference this) { }

    void a() { }

    void b() {}

    void c() {}

    /**
     * we want to annotate this method in a way that indicates it calls the b() method on its parameter x
     * @param x
     */
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
        // checker reports an error here though it's safe
        y.build();
    }

    static void calleeSecond() {
        CalleeInference y = new CalleeInference();
        y.a();
        y.c();
        callB(y);
        // checker reports an error here though it's safe
        y.build();
    }
}