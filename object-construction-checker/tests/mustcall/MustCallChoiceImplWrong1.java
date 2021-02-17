// A simple test that the extra obligations that MustCallChoice imposes are
// respected. This version gets it wrong by not assigning the MCC param
// to a field.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

public class MustCallChoiceImplWrong1 implements Closeable {

    final @Owning Closeable foo;

    // :: error: required.method.not.called
    public @MustCallChoice MustCallChoiceImplWrong1(@MustCallChoice Closeable foo) {
        this.foo = null;
    }

    @Override
    @EnsuresCalledMethods(value = {"this.foo"}, methods = {"close"})
    public void close() throws IOException {
        this.foo.close();
    }
}
