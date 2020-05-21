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

//    Foo makeFoo3() throws TerribleException{
//        Foo f = new Foo();
//
//        throw new TerribleException();
//
//    }

    void test1(){
        // :: error: missing.alwayscall
        Foo fw = makeFoo();
        Foo fc = makeFoo();
        Foo fe = makeFoo();
        try{
            fe.a();
            Foo ft = makeFoo();

            int n = 10/0;
        }catch (ArithmeticException e){

        }

        fc.a();

    }



    private class TerribleException extends Exception
    {
        public TerribleException()
        {
        }
    }


}