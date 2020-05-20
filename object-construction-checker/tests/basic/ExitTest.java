import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

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

    @CalledMethods({"a"}) Foo makeFoo2(){
        Foo f =  new Foo();
        f.a();
        return f;
    }

    void makeFooFinilize(){
        Foo f = new Foo();
        f.a();

    }

    void makeFooFinilize2(){
        // :: error: missing.alwayscall
        Foo m;
        m = new Foo();
        // :: error: missing.alwayscall
        Foo f = new Foo();
        String s = "name";
        f.b();

    }

    void testStoringInLocal() {
        // :: error: missing.alwayscall
        Foo foo = makeFoo();
    }

    void test1() {
        Foo foo = makeFoo2();
    }

//    void test2(Foo f){
//        Runnable r = new Runnable(){
//            void run(){
//                Foo f;
//            }
//        };
//    }

    void test3(Foo f){

    }



//    Foo testField;
//
//    void testStoringInField() {
//        // we should get an error here
//        testField = makeFoo();
//    }
}