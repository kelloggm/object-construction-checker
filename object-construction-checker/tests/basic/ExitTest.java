import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.returnsrcvr.qual.*;


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

    Foo makeFoo2(){
        Foo f =  new Foo();
        f.a();
        return f;
    }

    void makeFooFinilize(){
        Foo f = new Foo();
        f.a();

    }

    void makeFooFinilize2(){
        Foo m;  // report an error
        m = new Foo();
        Foo f = new Foo();  // report an error
        String s = "name";
        f.b();

    }

    void testStoringInLocal() {
        Foo foo = makeFoo();  // report an error
    }

    void test1() {
        Foo foo = makeFoo2();  // won't pass yet
    }


//    Foo testField;
//
//    void testStoringInField() {
//        // we should get an error here
//        testField = makeFoo();
//    }
}