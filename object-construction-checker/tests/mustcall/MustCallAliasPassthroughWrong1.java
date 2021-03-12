// A test that a class can extend another class with an MCA constructor,
// and have its own constructor be MCA as well.
// This version just throws away the input rather than passing it to the super constructor.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallAliasPassthroughWrong1 extends FilterInputStream {
    // :: error: required.method.not.called
    @MustCallAlias MustCallAliasPassthroughWrong1(@MustCallAlias InputStream is) {
        super(null);
    }
}
