// A test that a class can extend another class with an MCC constructor,
// and have its own constructor be MCC as well.
// This version passes the MCC param to another method instead of the passthrough constructor.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallChoicePassthroughWrong2 extends FilterInputStream {
    // :: error: ?
    @MustCallChoice MustCallChoicePassthroughWrong2(@MustCallChoice InputStream is) throws Exception {
        super(null);
        closeIS(is);
    }

    void closeIS(@Owning InputStream is) throws Exception {
        is.close();
    }
}
