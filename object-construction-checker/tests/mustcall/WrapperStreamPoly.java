// A simple test that must-call as a type annotation makes it so that so-called
// "polymorphic" streams like DataInputStream and DataOutputStream are treated as
// their constituent stream.

import java.io.*;
import org.checkerframework.checker.objectconstruction.qual.Owning;


class WrapperStreamPoly {
    void test_no_close_needed(@Owning ByteArrayInputStream b) {
        // b doesn't need to be closed, so neither does this stream.
        DataInputStream d = new DataInputStream(b);
    }

    void test_close_needed(@Owning InputStream b) {
        // :: error: required.method.not.called
        DataInputStream d = new DataInputStream(b);
    }
}
