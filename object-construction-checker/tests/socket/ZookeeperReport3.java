// Based on a Zookeeper false positive that requires unconnected socket support.
// This example is functionally equivalent to example #4, so I included it in this file
// ("createNewServerSocket").

import java.net.*;
import java.io.*;
import java.util.Optional;
import org.checkerframework.checker.unconnectedsocket.qual.Unconnected;

class ZookeeperReport3 {

    // This is a simpler version of case 3.
    Optional<ServerSocket> createServerSocket_easy(InetSocketAddress address, boolean portUnification, boolean sslQuorum) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(address);
            return Optional.of(serverSocket);
        } catch (IOException e) {
            System.err.println("Couldn't bind to " + address.toString() + e);
        }
        return Optional.empty();
    }

    Optional<ServerSocket> createServerSocket(InetSocketAddress address, boolean portUnification, boolean sslQuorum) {
        ServerSocket serverSocket;
        try {
            if (portUnification || sslQuorum) {
                serverSocket = new UnifiedServerSocket(portUnification);
            } else {
                serverSocket = new ServerSocket();
            }
            serverSocket.setReuseAddress(true);
            serverSocket.bind(address);
            return Optional.of(serverSocket);
        } catch (IOException e) {
            System.err.println("Couldn't bind to " + address.toString() + e);
        }
        return Optional.empty();
    }

    private ServerSocket createNewServerSocket(SocketAddress address, boolean portUnification, boolean sslQuorum) throws IOException {
        ServerSocket socket;

        if (portUnification) {
            System.out.println("Creating TLS-enabled quorum server socket");
            socket = new UnifiedServerSocket(true);
        } else if (sslQuorum) {
            System.out.println("Creating TLS-only quorum server socket");
            socket = new UnifiedServerSocket(false);
        } else {
            socket = new ServerSocket();
        }

        socket.setReuseAddress(true);
        socket.bind(address);

        return socket;
    }

    class UnifiedServerSocket extends ServerSocket {
        // A human has to verify that this class actually does produce an unconnected socket.
        @SuppressWarnings("unconnectedsocket:inconsistent.constructor.type")
        public @Unconnected UnifiedServerSocket(boolean b) throws IOException {
            super();
        }
    }
}
