// A test that a class can extend another class with an MCA constructor,
// and have its own constructor be MCA as well.
// This version just closes the MCA parameter, which isn't wrong so much as weird but I wanted a test for it.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallAliasPassthroughWrong4 extends FilterInputStream {
    // I mean I guess this return type is technically okay - it's too conservative (@Owning on the
    // param would be better) but I see no reason not to verify it.
    @MustCallAlias MustCallAliasPassthroughWrong4(@MustCallAlias InputStream is) throws Exception {
        super(null);
        is.close();
    }
}
