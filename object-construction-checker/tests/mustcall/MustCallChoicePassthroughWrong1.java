// A test that a class can extend another class with an MCC constructor,
// and have its own constructor be MCC as well.
// This version just throws away the input rather than passing it to the super constructor.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallChoicePassthroughWrong1 extends FilterInputStream {
    // :: error: required.method.not.called
    @MustCallChoice MustCallChoicePassthroughWrong1(@MustCallChoice InputStream is) {
        super(null);
    }
}
