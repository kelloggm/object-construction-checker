// A test that errors are correctly issued when re-assignments don't match the
// create obligation annotation on a method.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.checker.calledmethods.qual.*;
import java.io.*;

@MustCall("a")
class CreateObligationTargets {
    @Owning InputStream is1;

    @CreateObligation
    // :: error: incompatible.create.obligation
    static void resetObj1(CreateObligationTargets r) throws Exception {
        if (r.is1 == null) {
            r.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreateObligation("#2")
    // :: error: incompatible.create.obligation
    static void resetObj2(CreateObligationTargets r, CreateObligationTargets other) throws Exception {
        if (r.is1 == null) {
            r.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreateObligation("#1")
    static void resetObj3(CreateObligationTargets r, CreateObligationTargets other) throws Exception {
        if (r.is1 == null) {
            r.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreateObligation
    void resetObj4(CreateObligationTargets this, CreateObligationTargets other) throws Exception {
        if (is1 == null) {
            is1 = new FileInputStream("foo.txt");
        }
    }

    @CreateObligation
    // :: error: incompatible.create.obligation
    void resetObj5(CreateObligationTargets this, CreateObligationTargets other) throws Exception {
        if (other.is1 == null) {
            other.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreateObligation("#2")
    // :: error: incompatible.create.obligation
    void resetObj6(CreateObligationTargets this, CreateObligationTargets other) throws Exception {
        if (other.is1 == null) {
            other.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreateObligation("#1")
    void resetObj7(CreateObligationTargets this, CreateObligationTargets other) throws Exception {
        if (other.is1 == null) {
            other.is1 = new FileInputStream("foo.txt");
        }
    }

    @EnsuresCalledMethods(value="this.is1", methods="close")
    void a() throws Exception {
        is1.close();
    }
}
