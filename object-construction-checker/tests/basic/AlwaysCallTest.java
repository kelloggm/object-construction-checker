import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.returnsrcvr.qual.*;

/* The simplest inference test case Martin could think of */

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
        Foo f = new Foo();
        f.b();
        return f;
    }

    void makeFooFinilize3(){
        Foo f = new Foo();
        f.b();
        f.a();
    }


    void test1() {
        makeFoo().a();
    }

    void test2() {
        makeFoo().b().a();
    }

    void test3() {
        makeFoo().b(); // it reports an error
    }

    void test4() {
        makeFoo();   // it reports an error
    }

    void test5() {
        makeFooFinilize();
    }


    void test6() {
        makeFooFinilize2();
    }

    void test7() {
        makeFoo().c();
    }

}