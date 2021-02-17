// A test that a class can have multiple MCC constructors.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallChoicePassthroughThis extends FilterInputStream {
    @MustCallChoice MustCallChoicePassthroughThis(@MustCallChoice InputStream is) {
        super(is);
    }

    @MustCallChoice MustCallChoicePassthroughThis(@MustCallChoice InputStream is, int x) {
        this(is);
    }
}
