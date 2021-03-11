// This is a test for what happens when there's a missing MCC return type.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallAliasPassthroughWrong3 {
    // Both of these verify - it's okay to leave off the MCC param, because the return type is
    // owning - but the first one leads to imprecision at call sites.
    static InputStream missingMCC(@MustCallAlias InputStream is) {
        return is;
    }

    static @MustCallAlias InputStream withMCC(@MustCallAlias InputStream is) {
        return is;
    }

    // :: error: required.method.not.called
    void use_bad(@Owning InputStream is) throws Exception {
        InputStream is2 = missingMCC(is);
        is2.close();
    }

    void use_good(@Owning InputStream is) throws Exception {
        InputStream is2 = withMCC(is);
        is2.close();
    }
}
