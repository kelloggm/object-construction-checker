// A test that a class can extend another class with an MCC constructor,
// and have its own constructor be MCC as well.
// This version just closes the MCC parameter, which isn't wrong so much as weird but I wanted a test for it.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallChoicePassthroughWrong4 extends FilterInputStream {
    // I mean I guess this return type is technically okay - it's too conservative (@Owning on the
    // param would be better) but I see no reason not to verify it.
    @MustCallChoice MustCallChoicePassthroughWrong4(@MustCallChoice InputStream is) throws Exception {
        super(null);
        is.close();
    }
}
