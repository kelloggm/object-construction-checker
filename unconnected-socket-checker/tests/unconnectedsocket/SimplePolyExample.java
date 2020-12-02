// A simple example of the polymorphic qualifier, based on the method it was designed for.

import java.io.*;
import java.net.*;
import java.nio.channels.*;

import org.checkerframework.checker.unconnectedsocket.qual.*;

class SimplePolyExample {
    void test1() throws IOException {
        SocketChannel unconnectedChannel = SocketChannel.open();
        @Unconnected Socket unconnectedSocket = unconnectedChannel.socket();
    }

    void test2(SocketAddress addr) throws IOException {
        SocketChannel connectedChannel = SocketChannel.open(addr);
        // :: error: assignment.type.incompatible
        @Unconnected Socket connectedSocket = connectedChannel.socket();
    }
}
