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

    void exceptionalExitWrong() {
        // :: error: missing.alwayscall
        Foo fw = makeFoo();
        throw new RuntimeException();
    }

    void exceptionalExitCorrect() {
        Foo fw = new Foo();
        fw.a();
        throw new RuntimeException();
    }

}