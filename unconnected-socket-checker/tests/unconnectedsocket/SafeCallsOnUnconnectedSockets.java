// This test ensures that calling configuration methods on sockets doesn't cause them
// to be treated as possibly connected.

import java.io.*;
import java.net.*;
import java.nio.channels.*;

import org.checkerframework.checker.unconnectedsocket.qual.*;

class SafeCallsOnUnconnectedSockets {
    void test_socket() throws IOException {
        Socket sock = new Socket();
        sock.setKeepAlive(true);
        sock.setOOBInline(true);
        sock.setPerformancePreferences(1, 2, 3);
        sock.setReceiveBufferSize(1);
        sock.setReuseAddress(true);
        sock.setSendBufferSize(5);
        sock.setSoLinger(true, 10);
        sock.setSoTimeout(5);
        sock.setTcpNoDelay(true);
        sock.setTrafficClass(4);
        @Unconnected Socket sock2 = sock;
    }

    void test_server_socket() throws IOException {
        ServerSocket sock = new ServerSocket();
        sock.setPerformancePreferences(1, 2, 3);
        sock.setReceiveBufferSize(1);
        sock.setReuseAddress(true);
        sock.setSoTimeout(5);
        @Unconnected ServerSocket sock2 = sock;
    }

    void test_socket_channel() throws IOException {
        SocketChannel sock = SocketChannel.open();
        sock.setOption(StandardSocketOptions.SO_LINGER, 5);
        @Unconnected Socket sock2 = sock.socket();
    }

    void test_server_socket_channel() throws IOException {
        ServerSocketChannel sock = ServerSocketChannel.open();
        sock.setOption(StandardSocketOptions.SO_LINGER, 5);
        @Unconnected ServerSocket sock2 = sock.socket();
    }
}
