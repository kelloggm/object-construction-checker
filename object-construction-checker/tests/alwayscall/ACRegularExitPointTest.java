import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;
import java.util.function.Function;
import java.io.IOException;

class ACRegularExitPointTest {

    @AlwaysCall("a")
    class Foo {
        void a() {}
        @This Foo b() {
            return this;
        }
        void c(@CalledMethods({"a"}) Foo this) {}

    }


    Foo makeFoo(){
        return new Foo();
    }

    @CalledMethods({"a"}) Foo makeFooCallA(){
        Foo f =  new Foo();
        f.a();
        return f;
    }

    @EnsuresCalledMethods(value = "#1", methods = "a")
    void callA(Foo f) {
        f.a();
    }

    void makeFooFinalize(){
        Foo f = new Foo();
        f.a();
    }

    void makeFooFinalizeWrong(){
        Foo m;
        // :: error: missing.alwayscall
        m = new Foo();
        // :: error: missing.alwayscall
        Foo f = new Foo();
        f.b();
    }

    void testStoringInLocalWrong() {
        Foo foo = makeFoo();
    }

    void testStoringInLocalWrong2(){
        Foo f;
        f = makeFoo();
    }

    void testStoringInLocal() {

        Foo foo = makeFooCallA();
    }

    void testStoringInLocalWrong3() {
        // :: error: missing.alwayscall
        Foo foo = new Foo();
    }

    void emptyFuncWithFormalPram(Foo f){

    }

    void innerFunc(Foo f){
        Runnable r = new Runnable(){
            public void run(){
                Foo f;
            };
        };
        r.run();
    }


    void innerFuncWrong(Foo f){
        Runnable r = new Runnable(){
            public void run(){
                // :: error: missing.alwayscall
                Foo g = new Foo();
            };
        };
        r.run();
    }


    void innerFunc2(Foo f){
        Runnable r = new Runnable(){
            public void run(){
                Foo g = makeFoo();
                g.a();
            };
        };
        r.run();
    }


    void innerfunc3(){

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


    void ifElse(boolean b) {
        if (b) {
            Foo f1 = new Foo();
            f1.a();
        } else {
            // :: error: missing.alwayscall
            Foo f2 = new Foo();
        }

    }

    Foo ifElseWithReturnExit(boolean b, boolean c) {
        Foo f1 = makeFoo();
        // :: error: missing.alwayscall
        Foo f3 = new Foo();
        // :: error: missing.alwayscall
        Foo f4 = new Foo();

        if (b) {
            // :: error: missing.alwayscall
            Foo f2 = new Foo();
            if(c){
                f4.a();
            }else{
                f4.b();
            }
            return f1;
        }else {
            // :: error: missing.alwayscall
            Foo f2 = new Foo();
            f2 = new Foo();
            f2.a();
        }
        return f3;
    }


    void ifElseWithDeclaration(boolean b) {
        Foo f1;
        Foo f2;
        if (b) {
            f1 = new Foo();
            f1.a();
        } else {
            // :: error: missing.alwayscall
            f2 = new Foo();
        }
    }

    void ifElseWithInitialization(boolean b) {
        // :: error: missing.alwayscall
        Foo f2 = new Foo();
        Foo f11 = null;
        if (b) {
            f11 = makeFoo();
            f11.a();
        } else {
            // :: error: missing.alwayscall
            f2 = new Foo();
        }
    }

    void ifWithInitialization(boolean b) {
        // :: error: missing.alwayscall
        Foo f1 = new Foo();
        // :: error: missing.alwayscall
        Foo f2 = new Foo();
        if (b) {
            f1.a();
        }
    }

    void variableGoesOutOfScope(boolean b) {
        if (b) {
            Foo f1 = new Foo();
            f1.a();
        }
    }


    void ifWithNullInitialization(boolean b) {
        Foo f1 = null;
        Foo f2 = null;
        if (b) {
            f1 = new Foo();
            f1.a();
        } else {
            // :: error: missing.alwayscall
            f2 = new Foo();
        }
    }

    void variableInitializedWithNull() { Foo f = null; }

    void testLoop() {
        Foo f = null;
        while (true) {
            // :: error: missing.alwayscall
            f = new Foo();
        }
    }


    void overWrittingVarInLoop() {
        // :: error: missing.alwayscall
        Foo f = new Foo();
        while (true) {
            // :: error: missing.alwayscall
            f = new Foo();
        }
    }


    void loopWithNestedBranches(boolean b) {
        Foo f = null;
        while (true) {
            if (b) {
                // :: error: missing.alwayscall
                f = new Foo();
            } else {
                f = new Foo();
                f.a();
            }
        }
    }


    void replaceVarWithNull(boolean b, boolean c) {
        // :: error: missing.alwayscall
        Foo f = new Foo();
        if (b) {
            f = null;
        } else if (c) {
            f = null;
        } else {

        }
    }


    void ownershipTransfer(){
        Foo f1 = new Foo();
        //TODO this is a false positive but we're not going to handle it for now
        // :: error: missing.alwayscall
        Foo f2 = f1;
        Foo f3 = f2.b();
        f3.a();
    }

    void ownershipTransfer2(){
        Foo f1 = null;
        Foo f2 = f1;
    }

    void testECM(){
        Foo f = new Foo();
        callA(f);
    }


    void testFinallyBlock(boolean b) {
        Foo f = null;
        try {
            f = new Foo();
            if (true) {
                throw new IOException();
            }
        } catch (IOException e) {

        } finally {
            f.a();

        }
    }


}