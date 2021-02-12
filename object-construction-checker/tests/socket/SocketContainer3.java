// A simple class that has a Socket as an owning field.
// This test exists to check that we gracefully handle assignments.

import java.net.*;
import java.io.*;

import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("close")
class SocketContainer3 {
    @Owning Socket sock = null;

    public SocketContainer3(String host, int port) throws Exception {
        // It's not okay to assign to a field with an initializer, unless that initializer is a null literal.
        sock = new Socket(host, port);
    }

    @EnsuresCalledMethods(value="this.sock", methods="close")
    public void close() throws IOException {
        sock.close();
    }
}
