// A test that passing a local to an MCC super constructor is allowed.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallChoicePassthroughLocal extends FilterInputStream {
    MustCallChoicePassthroughLocal(File f) throws Exception {
        // This is safe - this MCC constructor of FilterInputStream means that the result of this
        // constructor - i.e. the caller - is taking ownership of this newly-created output stream.
        super(new FileInputStream(f));
    }

    static void test(File f) throws Exception {
        // :: error: required.method.not.called
        new MustCallChoicePassthroughLocal(f);
    }

    static void test_ok(File f) throws Exception {
        new MustCallChoicePassthroughLocal(f).close();
    }
}
