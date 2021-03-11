// A test that a class can extend another class with an MCC constructor,
// and have its own constructor be MCC as well.
// This version passes the MCC param to another method instead of the passthrough constructor.
// This is actually okay - the stream does get closed, if it needs to be closed - though the
// MCC annotation on the return type is super misleading and will lead to FPs. It would be better
// to annotate code like this with @Owning on the constructor.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallAliasPassthroughWrong2 extends FilterInputStream {
    @MustCallAlias MustCallAliasPassthroughWrong2(@MustCallAlias InputStream is) throws Exception {
        super(null);
        closeIS(is);
    }

    void closeIS(@Owning InputStream is) throws Exception {
        is.close();
    }
}
