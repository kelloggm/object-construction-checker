// A simple test verifys @MustCall counter works correctly
// Expected numMustCall = 7
// Expected numMustCallFailed = 3
// Expected numMustCallPassed = numMustCall - numMustCallFailed = 4

import java.net.*;
import java.io.*;

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

@MustCall("a")
class CounterTest {

    // Not counted
    static @MustCall({}) ServerSocket makeUnconnected() throws Exception {
        return new ServerSocket();
    }

    // numMustCall + 1 | numMustCallFailed + 1
    static void simple_Socket_test(SocketAddress sa) throws Exception {
        // :: error: required.method.not.called
        Socket s = new Socket();
        s.bind(sa);
    }

    // Not counted
    static void simple_Socket_test2(SocketAddress sa) throws Exception {
        Socket s = new Socket();
        // s.bind(sa);
    }

    // numMustCall + 1 | numMustCallFailed + 1
    static void simple_Socket_test3(SocketAddress sa) throws Exception {
        // :: error: required.method.not.called
        ServerSocket s = makeUnconnected();
        s.bind(sa);
    }

    // Not counted
    static void simple_Socket_test4(SocketAddress sa) throws Exception {
        ServerSocket s = makeUnconnected();
        // s.bind(sa);
    }

    // numMustCall + 1 | numMustCallPassed + 1
    void testLoop(String address, int port) {
        Socket s = null;
        while (true) {
            try {
                s = new Socket(address, port);
                s.close();
            } catch (IOException e) {

            }
        }
    }

    // Not counted
    void testLoop2() {
        Socket s = null;
        while (true) {
            s = new Socket();
        }
    }

    // numMustCall + 1  |  numMustCallFailed + 1
    void testLoopWong(String address, int port) {
        Socket s = null;
        while (true) {
            try {
                // :: error: required.method.not.called
                s = new Socket(address, port);
//                s.close();
            } catch (IOException e) {

            }
        }
    }

    // numMustCall + 3 | numMustCallPassed + 3
    void test_count_wrapper(@Owning Socket sock) throws IOException {
        try {
            InputStream is = sock.getInputStream();
            OutputStream os = sock.getOutputStream();
        } catch (IOException e) {

        } finally {
            sock.close();
        }
    }

    @MustCall("a") class Foo { }

    // not counted
    void test_non_java_star() {
        // :: error: required.method.not.called
        new Foo();
    }
}
