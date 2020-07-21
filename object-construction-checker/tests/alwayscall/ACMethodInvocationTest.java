import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;


class ACMethodInvocationTest {

    @AlwaysCall("a")
    class Foo {
        void a() {}
        @This Foo b() {
            return this;
        }
        void c() {}
    }


    Foo makeFoo(){
        return new Foo();
    }


    @CalledMethods({"a"}) Foo makeFooFinilize(){
        Foo f = new Foo();
        f.a();
        return f;
    }

    @CalledMethods({"b"}) Foo makeFooFinilize2(){
        Foo f = new Foo();
        f.b();
        return f;
    }

    //TODO
//    void CallMethodsInSequence() {
//        // won't pass yet due to dataflow issue
//        makeFoo().a();
//    }

    //TODO
//    void CallMethodsInSequence2() {
//        // won't pass yet due to dataflow issue
//        makeFoo().b().a();
//    }

    void testFluentAPIWrong() {
        // :: error: missing.alwayscall
        makeFoo().b();
    }

    void testFluentAPIWrong2() {
        // :: error: missing.alwayscall
        makeFoo();
    }

    void invokeMethodWithCallA() {
        makeFooFinilize();
    }


    void invokeMethodWithCallBWrong() {
        // :: error: missing.alwayscall
        makeFooFinilize2();
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

        testField3 = makeFooFinilize();
    }
}