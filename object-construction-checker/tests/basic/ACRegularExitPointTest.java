import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;
import java.util.function.Function;

class ACRegularExitPointTest {

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
        //TODO
        // won't pass yet
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



    // ???????
    void nestedFunc2(){

        Foo f = makeFoo();

        Function<Foo, Foo> innerfunc = st ->{
            st.a();
            Foo fn = makeFoo();
            return fn;
        };

        // :: error: missing.alwayscall
        innerfunc.apply(f);

    }


    void test7(){
        // :: error: missing.alwayscall
        Foo f = makeFoo();
        if(true){
            f.a();
        }else{
            f.c();
        }
    }

}