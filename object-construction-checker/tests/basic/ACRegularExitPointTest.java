import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;
import java.util.function.Function;
import java.io.IOException;

class ACRegularExitPointTest {

    @AlwaysCall("a")
    class Foo {

//        Foo() throws IOException {
//
//        }

        void a() {}
        @This Foo b() {
            return this;
        }
        void c(@CalledMethods({"a"}) Foo this) {}
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
        f.b();

    }

    void testStoringInLocal() {
        // :: error: missing.alwayscall
        Foo foo = makeFoo();
        Foo f;
    }

    void test1() {
        Foo foo = makeFoo2();
    }

    void test2(Foo f){

    }

    void test3(Foo f){
        Runnable r = new Runnable(){
            public void run(){
                Foo f;
            };
        };
        r.run();
    }


    void test4(Foo f){
        Runnable r = new Runnable(){
            public void run(){
                // :: error: missing.alwayscall
                Foo g = new Foo();
            };
        };
        r.run();
    }


    void test5(Foo f){
        Runnable r = new Runnable(){
            public void run(){
                Foo g = makeFoo();
                g.a();
            };
        };
        r.run();
    }


    void nestedFunc2(){

        Foo f = makeFoo();
        f.a();
        Function<Foo, @CalledMethods({"a"}) Foo> innerfunc = st ->{
            // :: error: missing.alwayscall
            Foo fn1 = new Foo();
            Foo fn2 = makeFoo();
            fn2.a();
            return fn2;
        };

        innerfunc.apply(f);

    }


    // return; is not counted in getReturnStatementStores so I changed it to return Foo
    void fooExitStoreCheck(boolean b) {
        if (b) {
            Foo f1 = new Foo();
            f1.a();
//            return;
        } else {
            // :: error: missing.alwayscall
            Foo f2 = new Foo();
//            return;
        }

    }

    Foo fooExitStoreCheck2(boolean b, boolean c) {
        // :: error: missing.alwayscall
        Foo f1 = makeFoo();
        Foo f3 = new Foo();

        if (b) {
//            f1.a();
            Foo f2 = new Foo();
            if(c){
                f3.a();
            }else{
                f3.b();
            }
            String h = "";
//            return f1;
        }else {
            // :: error: missing.alwayscall
            Foo f2 = new Foo();
//            return f4;
            String s = "";
        }

        return f3;
    }


    // localVariableValues : {b}
    void fooExitStoreCheck3(boolean b) {
        Foo f1, f2;
        if (b) {
            f1 = new Foo();
            f1.a();
        } else {
            f2 = new Foo();
        }
    }

    // localVariableValues : {b, f1, f2}
    // Annotations : {TOP, {a}, TOP}
    void fooExitStoreCheck4(boolean b) {
        Foo f1 = null, f2 = null;
        if (b) {
            f1 = new Foo();
            f1.a();
        } else {
            f2 = new Foo();
        }
    }

    // localVariableValues : {b, f1, f2}
    // Annotations : {TOP, TOP, TOP}
    void fooExitStoreCheck5(boolean b) {
        Foo f1 = new Foo(), f2 = new Foo();
        if (b) {
            f1.a();
        }
    }


    void fooExitStoreCheck6(boolean b) {
        Foo f1 = null, f2 = null;
        if (b) {
            f1 = new Foo();
            f1.a();
        } else {
            f2 = new Foo();
        }
        f1.c();
    }


    void test7(){
        // :: error: missing.alwayscall
        Foo f;
        f = makeFoo();
    }


    void test8() { Foo f = null; }

//    void test9E() {
//        try{
//            Foo f = new Foo();
//        }catch (IOException exception){
//
//        }
//
//    }

//    void test10E() throws IOException{
//        Foo f = new Foo();
//    }


}