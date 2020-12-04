// A simple test that the extra obligations that MustCallChoice imposes are
// respected.

// @skip-test until the checks are implemented

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

public class MustCallChoiceImpl implements Closeable {

    @Owning final Closeable foo;

    public @MustCallChoice MustCallChoiceImpl(@MustCallChoice Closeable foo) {
        this.foo = foo;
    }

    @Override
    public void close() throws IOException {
        this.foo.close();
    }
}
