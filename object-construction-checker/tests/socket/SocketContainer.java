// A simple class that has a Socket as an owning field.
// This test exists to check that we gracefully handle assignments 1)
// in the constructor and 2) to null.

import java.net.*;
import java.io.*;

import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@MustCall("close")
class SocketContainer {
    @Owning Socket sock;

    public SocketContainer(String host, int port) throws Exception {
        // It's okay to assign to uninitialized owning fields in the constructor.
        sock = new Socket(host, port);
        // But it's not okay to do it twice!
        // :: error: required.method.not.called
        sock = new Socket(host, port);
    }

    @EnsuresCalledMethods(value="this.sock", methods="close")
    public void close() throws IOException {
        sock.close();
        // It's okay to assign a field to null after its obligations have been fulfilled,
        // without inducing a reset.
        sock = null;
    }
}
