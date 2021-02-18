// A test that a class can extend another class with an MCC constructor,
// and have its own constructor be MCC as well.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

class MustCallChoicePassthrough extends FilterInputStream {
    @MustCallChoice MustCallChoicePassthrough(@MustCallChoice InputStream is) {
        super(is);
    }
}
