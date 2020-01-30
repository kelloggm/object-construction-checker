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

        @CalledMethods("a") MyClass a;
        @CalledMethods({"a", "b"}) MyClass aB;
        @CalledMethodsPredicate("a || b") MyClass aOrB;
        @CalledMethodsPredicate("a && b") MyClass aAndB;
        @CalledMethodsPredicate("a || b && c") MyClass bAndCOrA;
        @CalledMethodsPredicate("a || (b && c)") MyClass bAndCOrAParens;
        @CalledMethodsPredicate("a && b || c") MyClass aAndBOrC;
        @CalledMethodsPredicate("(a && b) || c") MyClass aAndBOrCParens;
        @CalledMethodsPredicate("(a || b) && c") MyClass aOrBAndC;
        @CalledMethodsPredicate("a && (b || c)") MyClass bOrCAndA;
        @CalledMethodsPredicate("b && c") MyClass bAndC;
        @CalledMethodsPredicate("(b && c)") MyClass bAndCParens;

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

        static void testAssignability2(@CalledMethodsPredicate("a && b") MyClass dAble) {
            dAble.c();
            dAble.d();
            dAble.e();
            dAble.f();
        }

        void testAllAssignability() {

            @CalledMethods("a") MyClass aLocal;
            @CalledMethodsPredicate("a || b") MyClass aOrBLocal;
            @CalledMethods({"a", "b"}) MyClass aBLocal;
            @CalledMethodsPredicate("a && b") MyClass aAndBLocal;
            @CalledMethodsPredicate("a || b && c") MyClass bAndCOrALocal;
            @CalledMethodsPredicate("a || (b && c)") MyClass bAndCOrAParensLocal;
            @CalledMethodsPredicate("a && b || c") MyClass aAndBOrCLocal;
            @CalledMethodsPredicate("(a && b) || c") MyClass aAndBOrCParensLocal;
            @CalledMethodsPredicate("(a || b) && c") MyClass aOrBAndCLocal;
            @CalledMethodsPredicate("a && (b || c)") MyClass bOrCAndALocal;
            @CalledMethodsPredicate("b && c") MyClass bAndCLocal;
            @CalledMethodsPredicate("(b && c)") MyClass bAndCParensLocal;

            aLocal = a;
            // :: error: assignment.type.incompatible
            aLocal = aOrB;
            aLocal = aB;
            aLocal = aAndB;
            // :: error: assignment.type.incompatible
            aLocal = bAndCOrA;
            // :: error: assignment.type.incompatible
            aLocal = bAndCOrAParens;
            // :: error: assignment.type.incompatible
            aLocal = aAndBOrC;
            // :: error: assignment.type.incompatible
            aLocal = aAndBOrCParens;
            // :: error: assignment.type.incompatible
            aLocal = aOrBAndC;
            aLocal = bOrCAndA;
            // :: error: assignment.type.incompatible
            aLocal = bAndC;
            // :: error: assignment.type.incompatible
            aLocal = bAndCParens;

            aOrBLocal = a;
            aOrBLocal = aOrB;
            aOrBLocal = aB;
            aOrBLocal = aAndB;
            aOrBLocal = bAndCOrA;
            aOrBLocal = bAndCOrAParens;
            // :: error: assignment.type.incompatible
            aOrBLocal = aAndBOrC;
            // :: error: assignment.type.incompatible
            aOrBLocal = aAndBOrCParens;
            aOrBLocal = aOrBAndC;
            aOrBLocal = bOrCAndA;
            aOrBLocal = bAndC;
            aOrBLocal = bAndCParens;

            // :: error: (assignment.type.incompatible)
            aBLocal = a;
            // :: error: (assignment.type.incompatible)
            aBLocal = aOrB;
            aBLocal = aB;
            aBLocal = aAndB;
            // :: error: (assignment.type.incompatible)
            aBLocal = bAndCOrA;
            // :: error: (assignment.type.incompatible)
            aBLocal = bAndCOrAParens;
            // :: error: (assignment.type.incompatible)
            aBLocal = aAndBOrC;
            // :: error: (assignment.type.incompatible)
            aBLocal = aAndBOrCParens;
            // :: error: (assignment.type.incompatible)
            aBLocal = aOrBAndC;
            // :: error: (assignment.type.incompatible)
            aBLocal = bOrCAndA;
            // :: error: (assignment.type.incompatible)
            aBLocal = bAndC;
            // :: error: (assignment.type.incompatible)
            aBLocal = bAndCParens;

            // :: error: (assignment.type.incompatible)
            aAndBLocal = a;
            // :: error: (assignment.type.incompatible)
            aAndBLocal = aOrB;
            aAndBLocal = aB;
            aAndBLocal = aAndB;
            // :: error: (assignment.type.incompatible)
            aAndBLocal = bAndCOrA;
            // :: error: (assignment.type.incompatible)
            aAndBLocal = bAndCOrAParens;
            // :: error: (assignment.type.incompatible)
            aAndBLocal = aAndBOrC;
            // :: error: (assignment.type.incompatible)
            aAndBLocal = aAndBOrCParens;
            // :: error: (assignment.type.incompatible)
            aAndBLocal = aOrBAndC;
            // :: error: (assignment.type.incompatible)
            aAndBLocal = bOrCAndA;
            // :: error: (assignment.type.incompatible)
            aAndBLocal = bAndC;
            // :: error: (assignment.type.incompatible)
            aAndBLocal = bAndCParens;

            bAndCOrALocal = a;
            // :: error: (assignment.type.incompatible)
            bAndCOrALocal = aOrB;
            bAndCOrALocal = aB;
            bAndCOrALocal = aAndB;
            bAndCOrALocal = bAndCOrA;
            bAndCOrALocal = bAndCOrAParens;
            // :: error: (assignment.type.incompatible)
            bAndCOrALocal = aAndBOrC;
            // :: error: (assignment.type.incompatible)
            bAndCOrALocal = aAndBOrCParens;
            bAndCOrALocal = aOrBAndC;
            bAndCOrALocal = bOrCAndA;
            bAndCOrALocal = bAndC;
            bAndCOrALocal = bAndCParens;

            bAndCOrAParensLocal = a;
            // :: error: (assignment.type.incompatible)
            bAndCOrAParensLocal = aOrB;
            bAndCOrAParensLocal = aB;
            bAndCOrAParensLocal = aAndB;
            bAndCOrAParensLocal = bAndCOrA;
            bAndCOrAParensLocal = bAndCOrAParens;
            // :: error: (assignment.type.incompatible)
            bAndCOrAParensLocal = aAndBOrC;
            // :: error: (assignment.type.incompatible)
            bAndCOrAParensLocal = aAndBOrCParens;
            bAndCOrAParensLocal = aOrBAndC;
            bAndCOrAParensLocal = bOrCAndA;
            bAndCOrAParensLocal = bAndC;
            bAndCOrAParensLocal = bAndCParens;

            // :: error: (assignment.type.incompatible)
            aAndBOrCLocal = a;
            // :: error: (assignment.type.incompatible)
            aAndBOrCLocal = aOrB;
            aAndBOrCLocal = aB;
            aAndBOrCLocal = aAndB;
            // :: error: (assignment.type.incompatible)
            aAndBOrCLocal = bAndCOrA;
            // :: error: (assignment.type.incompatible)
            aAndBOrCLocal = bAndCOrAParens;
            aAndBOrCLocal = aAndBOrC;
            aAndBOrCLocal = aAndBOrCParens;
            aAndBOrCLocal = aOrBAndC;
            aAndBOrCLocal = bOrCAndA;
            aAndBOrCLocal = bAndC;
            aAndBOrCLocal = bAndCParens;

            // :: error: (assignment.type.incompatible)
            aAndBOrCParensLocal = a;
            // :: error: (assignment.type.incompatible)
            aAndBOrCParensLocal = aOrB;
            aAndBOrCParensLocal = aB;
            aAndBOrCParensLocal = aAndB;
            // :: error: (assignment.type.incompatible)
            aAndBOrCParensLocal = bAndCOrA;
            // :: error: (assignment.type.incompatible)
            aAndBOrCParensLocal = bAndCOrAParens;
            aAndBOrCParensLocal = aAndBOrC;
            aAndBOrCParensLocal = aAndBOrCParens;
            aAndBOrCParensLocal = aOrBAndC;
            aAndBOrCParensLocal = bOrCAndA;
            aAndBOrCParensLocal = bAndC;
            aAndBOrCParensLocal = bAndCParens;

            // :: error: (assignment.type.incompatible)
            aOrBAndCLocal = a;
            // :: error: (assignment.type.incompatible)
            aOrBAndCLocal = aOrB;
            // :: error: (assignment.type.incompatible)
            aOrBAndCLocal = aB;
            // :: error: (assignment.type.incompatible)
            aOrBAndCLocal = aAndB;
            // :: error: (assignment.type.incompatible)
            aOrBAndCLocal = bAndCOrA;
            // :: error: (assignment.type.incompatible)
            aOrBAndCLocal = bAndCOrAParens;
            // :: error: (assignment.type.incompatible)
            aOrBAndCLocal = aAndBOrC;
            // :: error: (assignment.type.incompatible)
            aOrBAndCLocal = aAndBOrCParens;
            aOrBAndCLocal = aOrBAndC;
            // :: error: (assignment.type.incompatible)
            aOrBAndCLocal = bOrCAndA;
            aOrBAndCLocal = bAndC;
            aOrBAndCLocal = bAndCParens;

            // :: error: (assignment.type.incompatible)
            bOrCAndALocal = a;
            // :: error: (assignment.type.incompatible)
            bOrCAndALocal = aOrB;
            bOrCAndALocal = aB;
            bOrCAndALocal = aAndB;
            // :: error: (assignment.type.incompatible)
            bOrCAndALocal = bAndCOrA;
            // :: error: (assignment.type.incompatible)
            bOrCAndALocal = bAndCOrAParens;
            // :: error: (assignment.type.incompatible)
            bOrCAndALocal = aAndBOrC;
            // :: error: (assignment.type.incompatible)
            bOrCAndALocal = aAndBOrCParens;
            // :: error: (assignment.type.incompatible)
            bOrCAndALocal = aOrBAndC;
            bOrCAndALocal = bOrCAndA;
            // :: error: (assignment.type.incompatible)
            bOrCAndALocal = bAndC;
            // :: error: (assignment.type.incompatible)
            bOrCAndALocal = bAndCParens;

            // :: error: (assignment.type.incompatible)
            bAndCLocal = a;
            // :: error: (assignment.type.incompatible)
            bAndCLocal = aOrB;
            // :: error: (assignment.type.incompatible)
            bAndCLocal = aB;
            // :: error: (assignment.type.incompatible)
            bAndCLocal = aAndB;
            // :: error: (assignment.type.incompatible)
            bAndCLocal = bAndCOrA;
            // :: error: (assignment.type.incompatible)
            bAndCLocal = bAndCOrAParens;
            // :: error: (assignment.type.incompatible)
            bAndCLocal = aAndBOrC;
            // :: error: (assignment.type.incompatible)
            bAndCLocal = aAndBOrCParens;
            // :: error: (assignment.type.incompatible)
            bAndCLocal = aOrBAndC;
            // :: error: (assignment.type.incompatible)
            bAndCLocal = bOrCAndA;
            bAndCLocal = bAndC;
            bAndCLocal = bAndCParens;

            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = a;
            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = aOrB;
            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = aB;
            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = aAndB;
            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = bAndCOrA;
            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = bAndCOrAParens;
            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = aAndBOrC;
            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = aAndBOrCParens;
            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = aOrBAndC;
            // :: error: (assignment.type.incompatible)
            bAndCParensLocal = bOrCAndA;
            bAndCParensLocal = bAndC;
            bAndCParensLocal = bAndCParens;
        }
    }
}
