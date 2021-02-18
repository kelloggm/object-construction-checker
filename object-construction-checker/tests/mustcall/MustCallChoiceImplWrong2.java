// A simple test that the extra obligations that MustCallChoice imposes are
// respected. This version gets it wrong by assigning the MCC param to a non-owning
// field.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.*;

public class MustCallChoiceImplWrong2 implements Closeable {

    final /*@Owning*/ Closeable foo;

    // :: error: required.method.not.called
    public @MustCallChoice MustCallChoiceImplWrong2(@MustCallChoice Closeable foo) {
        this.foo = foo;
    }

    @Override
    public void close() {

    }
}
