// A simple test that the extra obligations that MustCallChoice imposes are
// respected. Identical to MustCallChoiceImpl.java except the @Owning annotation
// on foo has been removed, making this unverifiable.

// @skip-test until the checks are implemented

import org.checkerframework.checker.mustcall.qual.*;
import java.io.*;

public class MustCallChoiceImplNoOwning implements Closeable {

    final Closeable foo;

    // :: error: mustcall.choice.invalid
    public @MustCallChoice MustCallChoiceImplNoOwning(@MustCallChoice Closeable foo) {
        this.foo = foo;
    }

    @Override
    public void close() throws IOException {
        this.foo.close();
    }
}
