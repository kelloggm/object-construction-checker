import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.returnsrcvr.qual.*;

/* The simplest inference test case Martin could think of */

class ExitTest {

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


    void makeFooFinilize(){
        Foo f = new Foo();
        f.a();

    }

    void makeFooFinilize2(){
        Foo m;
        m = new Foo();
        Foo f = new Foo();
        String s = "name";
        f.b();

    }

//    void test1() {
//        // won't pass yet due to dataflow issue
//        makeFoo().a();
//    }
//
//    void test2() {
//        // won't pass yet due to dataflow issue
//        makeFoo().b().a();
//    }
//
//    void test3() {
//        makeFoo().b(); // it reports an error
//    }
//
//    void test4() {
//        makeFoo();   // it reports an error
//    }
//
//    void test5() {
//        makeFooFinilize();
//    }
//
//
//    void test6() {
//        makeFooFinilize2();   // it reports an error
//    }
//
//    void test7() {
//        makeFoo().c();    // it reports an error
//    }
//
//    void testStoringInLocal() {
//        // won't detect error until we check for variables going out of scope
//        Foo foo = makeFoo();
//    }
//
//    Foo testField;
//
//    void testStoringInField() {
//        // we should get an error here
//        testField = makeFoo();
//    }
}