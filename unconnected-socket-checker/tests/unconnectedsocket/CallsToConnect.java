// A simple test that calls to connect or methods that might call connect
// change the type of an unconnected socket.

import java.io.*;
import java.net.*;

import org.checkerframework.checker.unconnectedsocket.qual.*;

class CallsToConnect {
    void simple_test(SocketAddress endpoint) throws IOException {
        Socket sock = new Socket();
        sock.connect(endpoint);
        // :: error: assignment.type.incompatible
        @Unconnected Socket sock2 = sock;
    }

    static void someNonPureMethod() { }

    void simple_test_no_unrefine(SocketAddress endpoint) throws IOException {
        Socket sock = new Socket();
        sock.connect(endpoint);
        // if the call to connect is just unrefining in the store, this call might make
        // the next assignment succeed
        someNonPureMethod();
        // :: error: assignment.type.incompatible
        @Unconnected Socket sock2 = sock;
    }

    static void sockConnect(Socket sock, SocketAddress endpoint) {
        // the checker should assume that this method (or any method) connects the socket, regardless
        // of what its contents are
    }

    void simple_test_delegate(SocketAddress endpoint) throws IOException {
        Socket sock = new Socket();
        sockConnect(sock, endpoint);
        // :: error: assignment.type.incompatible
        @Unconnected Socket sock2 = sock;
    }

    private Socket mySock;

    void simple_test_field(SocketAddress endpoint) throws IOException {
        Socket sock = new Socket();
        // another thread could connect mySock, or anything else could happen.
        // also, non-possibly-connected fields don't make any sense
        mySock = sock;
        // :: error: assignment.type.incompatible
        @Unconnected Socket sock2 = sock;
    }

    // all assignments to this that aren't null should fail - this doesn't make sense
    // :: error: unconnected.field
    private @Unconnected Socket mySock2 = null;

    void simple_test_field2(SocketAddress endpoint) throws IOException {
        Socket sock = new Socket();
        mySock2 = sock;
        // :: error: assignment.type.incompatible
        @Unconnected Socket sock2 = sock;
    }
}
