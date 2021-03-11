// This test checks that a chain of several MCC methods can be verified, and that messing up the chain
// leads to errors.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallAliasPassthroughChain {

    static @MustCallAlias InputStream withMCC(@MustCallAlias InputStream is) {
        return is;
    }

    static @MustCallAlias InputStream chain1(@MustCallAlias InputStream is) {
        return withMCC(is);
    }

    static @MustCallAlias InputStream chain2(@MustCallAlias InputStream is) {
        InputStream s = withMCC(is);
        return s;
    }

    static @MustCallAlias InputStream chain3(@MustCallAlias InputStream is) {
        return withMCC(chain1(is));
    }

    static @MustCallAlias InputStream chain4(@MustCallAlias InputStream is) {
        return withMCC(chain1(chain3(is)));
    }

    static @MustCallAlias InputStream chain5(@MustCallAlias InputStream is) {
        InputStream s = withMCC(chain1(is));
        return s;
    }

    // :: error: required.method.not.called
    static @MustCallAlias InputStream chain_bad1(@MustCallAlias InputStream is) {
        InputStream s = withMCC(chain1(is));
        return null;
    }

    // :: error: required.method.not.called
    static @MustCallAlias InputStream chain_bad2(@MustCallAlias InputStream is) {
        withMCC(chain1(is));
        return null;
    }

    // :: error: required.method.not.called
    static @MustCallAlias InputStream chain_bad3(@MustCallAlias InputStream is, boolean b) {
        return b ? null : withMCC(chain1(is));
    }
}
