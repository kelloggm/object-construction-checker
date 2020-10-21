// This test case was intended to simulate the code below, which issued
// a false positive at the call to LOG.warn() because the socket is @MustCall("close"):
//
//     synchronized void closeSockets() {
//       for (ServerSocket serverSocket : serverSockets) {
//           if (!serverSocket.isClosed()) {
//               try {
//                   serverSocket.close();
//               } catch (IOException e) {
//                   LOG.warn("Ignoring unexpected exception during close {}", serverSocket, e);
//               }
//           }
//       }
//    }
//
// This test now also tests for other interactions between NotOwning and MustCall annotations, too.

import java.net.ServerSocket;
import java.nio.channels.SocketChannel;
import java.io.IOException;

import org.checkerframework.checker.objectconstruction.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.MustCall;

class LogTheSocket {

    @NotOwning ServerSocket s;

    @MustCall("") Object s2;

    void testAssign(ServerSocket s1) {
        s = s1;
        // :: error: assignment.type.incompatible
        s2 = s1;
    }

    void logVarargs(@NotOwning String s, @NotOwning Object... objects) { }

    void logNoVarargs(@NotOwning String s, @NotOwning Object object) { }

    void test(ServerSocket serverSocket) {
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logVarargs("Ignoring unexpected exception during close {}", serverSocket, e);
            }
        }
    }

    void test2(ServerSocket serverSocket) {
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logNoVarargs("Ignoring unexpected exception during close {}", serverSocket);
            }
        }
    }

    // This is (mostly) copied from ACSocketTest; under a previous implementation of the ownership-transfer scheme,
    // it caused false positive warnings from the MustCall checker.
    SocketChannel createSock() throws IOException {
        SocketChannel sock;
        sock = SocketChannel.open();
        sock.configureBlocking(false);
        sock.socket().setSoLinger(false, -1);
        sock.socket().setTcpNoDelay(true);
        return sock;
    }
}
