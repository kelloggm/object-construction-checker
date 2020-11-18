// Simple tests of @MustCallChoice functionality on wrapper streams.

import java.io.*;
import org.checkerframework.checker.objectconstruction.qual.*;

class MustCallChoiceExamples {
    void test_close_wrapper(@Owning InputStream b) throws IOException {
        DataInputStream d = new DataInputStream(b);
        d.close();
    }

    void test_close_nonwrapper(@Owning InputStream b) throws IOException {
        DataInputStream d = new DataInputStream(b);
        b.close();
    }

    void test_no_close(@Owning InputStream b) {
        // :: error: required.method.not.called
        DataInputStream d = new DataInputStream(b);
    }
}
