import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;


class AlwaysCallTest {

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
        // :: error: missing.alwayscall
        Foo f = new Foo();
        // won't pass yet as we don't yet handle it
        f.b();
        return f;
    }

//    void makeFooFinilize3(){
//        Foo f = new Foo();
//        f.b();
//        f.a();
//    }


    void test1() {
        // won't pass yet due to dataflow issue
        makeFoo().a();
    }

    void test2() {
        // won't pass yet due to dataflow issue
        // :: error: missing.alwayscall
        makeFoo().b().a();
    }

    void test3() {
        // :: error: missing.alwayscall
        makeFoo().b();
    }

    void test4() {
        // :: error: missing.alwayscall
        makeFoo();
    }

    void test5() {
        makeFooFinilize();
    }


    void test6() {
        // :: error: missing.alwayscall
        makeFooFinilize2();
    }

    void test7() {
        // :: error: missing.alwayscall
        makeFoo().c();
    }

    void testStoringInLocal() {
        // :: error: missing.alwayscall
        Foo foo = makeFoo();
    }

    Foo testField;

    void testStoringInField() {
        // we should get an error here
        testField = makeFoo();
    }
}