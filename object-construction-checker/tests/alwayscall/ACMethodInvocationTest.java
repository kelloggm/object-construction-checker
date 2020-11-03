import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;

class ACMethodInvocationTest {

    @AlwaysCall("a")
    class Foo {
        void a() {}
        @This Foo b() {
            return this;
        }
        void c() {}
    }


    @Owning Foo makeFoo(){
        return new Foo();
    }


    @CalledMethods({"a"}) Foo makeFooFinalize(){
        Foo f = new Foo();
        f.a();
        return f;
    }

    @Owning @CalledMethods({"b"}) Foo makeFooFinalize2(){
        Foo f = new Foo();
        f.b();
        return f;
    }

    //TODO
    void CallMethodsInSequence() {
        // won't pass yet due to dataflow issue
        // :: error: missing.alwayscall
        makeFoo().a();
    }

    //TODO
    void CallMethodsInSequence2() {
        // won't pass yet due to dataflow issue
        // :: error: missing.alwayscall
        makeFoo().b().a();
    }

    void testFluentAPIWrong() {
        // :: error: missing.alwayscall
        makeFoo().b();
    }

    void testFluentAPIWrong2() {
        // :: error: missing.alwayscall
        makeFoo();
    }

    void invokeMethodWithCallA() {
        makeFooFinalize();
    }


    void invokeMethodWithCallBWrong() {
        // :: error: missing.alwayscall
        makeFooFinalize2();
    }

    void invokeMethodAndCallCWrong() {
        // :: error: missing.alwayscall
        makeFoo().c();
    }

    Foo returnMakeFoo(){
        return makeFoo();
    }

    Foo testField1;
    Foo testField2;
    Foo testField3;

    void testStoringInField() {
        // :: error: missing.alwayscall
        testField1 = makeFoo();
        // :: error: missing.alwayscall
        testField2 = new Foo();

        testField3 = makeFooFinalize();
    }

    void tryCatchFinally(){
        Foo f = null;
        try {
            f = new Foo();
            try {
                throw new RuntimeException();
            } catch (Exception e) {

            }
        } finally {
            f.a();
        }
    }
}