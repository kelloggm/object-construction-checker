import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class ACExceptionalExitPointTest {

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


    //TODO
//    void test1() {
//        Foo fw = makeFoo();
//        throw new RuntimeException();
//    }



    Foo returnMakeFoo(){
        return makeFoo();
    }






    //TODO
    void exceptionalExit() {
        Foo fw1 = new Foo();
        Foo fw = new Foo();
        fw.a();
        throw new RuntimeException();
    }

}