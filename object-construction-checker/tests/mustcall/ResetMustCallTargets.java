// A test that errors are correctly issued when re-assignments don't match the
// reset must call annotation on a method.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.checker.calledmethods.qual.*;
import java.io.*;

@MustCall("a")
class ResetMustCallTargets {
    @Owning InputStream is1;

    @ResetMustCall
    // :: error: incompatible.reset.mustcall
    static void resetObj1(ResetMustCallTargets r) throws Exception {
        if (r.is1 == null) {
            r.is1 = new FileInputStream("foo.txt");
        }
    }

    @ResetMustCall("#2")
    // :: error: incompatible.reset.mustcall
    static void resetObj2(ResetMustCallTargets r, ResetMustCallTargets other) throws Exception {
        if (r.is1 == null) {
            r.is1 = new FileInputStream("foo.txt");
        }
    }

    @ResetMustCall("#1")
    static void resetObj3(ResetMustCallTargets r, ResetMustCallTargets other) throws Exception {
        if (r.is1 == null) {
            r.is1 = new FileInputStream("foo.txt");
        }
    }

    @ResetMustCall
    void resetObj4(ResetMustCallTargets this, ResetMustCallTargets other) throws Exception {
        if (is1 == null) {
            is1 = new FileInputStream("foo.txt");
        }
    }

    @ResetMustCall
    // :: error: incompatible.reset.mustcall
    void resetObj5(ResetMustCallTargets this, ResetMustCallTargets other) throws Exception {
        if (other.is1 == null) {
            other.is1 = new FileInputStream("foo.txt");
        }
    }

    @ResetMustCall("#2")
    // :: error: incompatible.reset.mustcall
    void resetObj6(ResetMustCallTargets this, ResetMustCallTargets other) throws Exception {
        if (other.is1 == null) {
            other.is1 = new FileInputStream("foo.txt");
        }
    }

    @ResetMustCall("#1")
    void resetObj7(ResetMustCallTargets this, ResetMustCallTargets other) throws Exception {
        if (other.is1 == null) {
            other.is1 = new FileInputStream("foo.txt");
        }
    }

    @EnsuresCalledMethods(value="this.is1", methods="close")
    void a() throws Exception {
        is1.close();
    }
}
