// A test that various socket constructors default correctly.

import java.io.*;
import java.net.*;
import java.nio.channels.*;

import org.checkerframework.checker.unconnectedsocket.qual.*;

class SimpleSockets {
    void test_java_net_socket_noargs() throws IOException {
        @Unconnected Socket sock = new Socket();
    }

    void test_java_net_socket_args1(String addr, int port) throws IOException {
        // :: error: assignment.type.incompatible
        @Unconnected Socket sock = new Socket(addr, port);
    }

    void test_java_net_socket_args2(InetAddress address, int port) throws IOException {
        // :: error: assignment.type.incompatible
        @Unconnected Socket sock = new Socket(address, port);
    }

    void test_java_net_socket_args3(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
        // :: error: assignment.type.incompatible
        @Unconnected Socket sock = new Socket(address, port, localAddr, localPort);
    }

    void test_java_net_socket_args4(Proxy proxy) throws IOException {
        @Unconnected Socket sock = new Socket(proxy);
    }

    void test_java_net_socket_args5(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        // :: error: assignment.type.incompatible
        @Unconnected Socket sock = new Socket(host, port, localAddr, localPort);
    }

    void test_java_net_serverSocket_noargs() throws IOException {
        @Unconnected ServerSocket sock = new ServerSocket();
    }

    void test_java_net_serverSocket_args1(int port) throws IOException {
        // :: error: assignment.type.incompatible
        @Unconnected ServerSocket sock = new ServerSocket(port);
    }

    void test_java_net_serverSocket_args2(int port, int backlog) throws IOException {
        // :: error: assignment.type.incompatible
        @Unconnected ServerSocket sock = new ServerSocket(port, backlog);
    }

    void test_java_net_serverSocket_args3(int port, int backlog, InetAddress bindAddr) throws IOException {
        // :: error: assignment.type.incompatible
        @Unconnected ServerSocket sock = new ServerSocket(port, backlog, bindAddr);
    }

    void test_java_nio_channels_socketchannel_open_noargs() throws IOException {
        @Unconnected SocketChannel sock = SocketChannel.open();
    }

    void test_java_nio_channels_socketchannel_open_args(SocketAddress addr) throws IOException {
        // :: error: assignment.type.incompatible
        @Unconnected SocketChannel sock = SocketChannel.open(addr);
    }
}
