// Simple tests of @MustCallChoice functionality on wrapper streams.

import java.io.*;
import java.net.*;
import org.checkerframework.checker.objectconstruction.qual.*;

class MustCallChoiceExamples {

    void test_two_locals(String address) throws IOException {
        Socket socket = new Socket( address, 80 );
        DataInputStream d = new DataInputStream(socket.getInputStream());
        d.close();
    }

    void test_close_wrapper(@Owning InputStream b) throws IOException {
        DataInputStream dSpecial = new DataInputStream(b);
        dSpecial.close();
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
