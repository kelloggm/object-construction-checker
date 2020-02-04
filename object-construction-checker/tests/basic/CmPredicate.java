import org.checkerframework.checker.objectconstruction.qual.*;

class CmPredicate {

    void testOr1() {
        MyClass m1 = new MyClass();

        // :: error: method.invocation.invalid
        m1.c();
    }

    void testOr2() {
        MyClass m1 = new MyClass();

        m1.a();
        m1.c();
    }

    void testOr3() {
        MyClass m1 = new MyClass();

        m1.b();
        m1.c();
    }

    void testAnd1() {
        MyClass m1 = new MyClass();

        // :: error: method.invocation.invalid
        m1.d();
    }

    void testAnd2() {
        MyClass m1 = new MyClass();

        m1.a();
        // :: error: method.invocation.invalid
        m1.d();
    }

    void testAnd3() {
        MyClass m1 = new MyClass();

        m1.b();
        // :: error: method.invocation.invalid
        m1.d();
    }

    void testAnd4() {
        MyClass m1 = new MyClass();

        m1.a();
        m1.c();
        // :: error: method.invocation.invalid
        m1.d();
    }

    void testAnd5() {
        MyClass m1 = new MyClass();

        m1.a();
        m1.b();
        m1.d();
    }

    void testAnd6() {
        MyClass m1 = new MyClass();

        m1.a();
        m1.b();
        m1.c();
        m1.d();
    }

    void testAndOr1() {
        MyClass m1 = new MyClass();

        // :: error: method.invocation.invalid
        m1.e();
    }

    void testAndOr2() {
        MyClass m1 = new MyClass();

        m1.a();
        m1.e();
    }

    void testAndOr3() {
        MyClass m1 = new MyClass();

        m1.b();
        // :: error: method.invocation.invalid
        m1.e();
    }

    void testAndOr4() {
        MyClass m1 = new MyClass();

        m1.b();
        m1.c();
        m1.e();
    }

    void testAndOr5() {
        MyClass m1 = new MyClass();

        m1.a();
        m1.b();
        m1.c();
        m1.d();
        m1.e();
    }

    void testPrecedence1() {
        MyClass m1 = new MyClass();

        // :: error: method.invocation.invalid
        m1.f();
    }

    void testPrecedence2() {
        MyClass m1 = new MyClass();

        m1.a();
        // :: error: method.invocation.invalid
        m1.f();
    }

    void testPrecedence3() {
        MyClass m1 = new MyClass();

        m1.b();
        // :: error: method.invocation.invalid
        m1.f();
    }

    void testPrecedence4() {
        MyClass m1 = new MyClass();

        m1.a();
        m1.b();
        m1.f();
    }

    void testPrecedence5() {
        MyClass m1 = new MyClass();

        m1.a();
        m1.c();
        m1.f();
    }

    void testPrecedence6() {
        MyClass m1 = new MyClass();

        m1.b();
        m1.c();
        m1.f();
    }

    private static class MyClass {
        void a() { }
        void b() { }
        void c(@CalledMethodsPredicate("a || b") MyClass this) { }
        void d(@CalledMethodsPredicate("a && b") MyClass this) { }
        void e(@CalledMethodsPredicate("a || (b && c)") MyClass this) { }
        void f(@CalledMethodsPredicate("a && b || c") MyClass this) { }

        static void testAssignability1(@CalledMethodsPredicate("a || b") MyClass cAble) {
            cAble.c();
            // :: error: method.invocation.invalid
            cAble.d();
            // :: error: method.invocation.invalid
            cAble.e();
            // :: error: method.invocation.invalid
            cAble.f();
        }
    }
}