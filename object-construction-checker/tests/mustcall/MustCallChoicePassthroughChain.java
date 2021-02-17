// This test checks that a chain of several MCC methods can be verified, and that messing up the chain
// leads to errors.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallChoicePassthroughChain {

    static @MustCallChoice InputStream withMCC(@MustCallChoice InputStream is) {
        return is;
    }

    static @MustCallChoice InputStream chain1(@MustCallChoice InputStream is) {
        return withMCC(is);
    }

    static @MustCallChoice InputStream chain2(@MustCallChoice InputStream is) {
        InputStream s = withMCC(is);
        return s;
    }

    static @MustCallChoice InputStream chain3(@MustCallChoice InputStream is) {
        return withMCC(chain1(is));
    }

    static @MustCallChoice InputStream chain4(@MustCallChoice InputStream is) {
        return withMCC(chain1(chain3(is)));
    }

    static @MustCallChoice InputStream chain5(@MustCallChoice InputStream is) {
        InputStream s = withMCC(chain1(is));
        return s;
    }

    // :: error: required.method.not.called
    static @MustCallChoice InputStream chain_bad1(@MustCallChoice InputStream is) {
        InputStream s = withMCC(chain1(is));
        return null;
    }

    // :: error: required.method.not.called
    static @MustCallChoice InputStream chain_bad2(@MustCallChoice InputStream is) {
        withMCC(chain1(is));
        return null;
    }

    // :: error: required.method.not.called
    static @MustCallChoice InputStream chain_bad3(@MustCallChoice InputStream is, boolean b) {
        return b ? null : withMCC(chain1(is));
    }
}
