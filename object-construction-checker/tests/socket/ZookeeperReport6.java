// Based on a Zookeeper false positive that requires unconnected socket support.

// @skip-test

import java.nio.channels.SocketChannel;
import java.io.IOException;

class ZookeeperReport6 {
    SocketChannel createSock() throws IOException {
        SocketChannel sock;
        sock = SocketChannel.open();
        // An error is currently issued here, because the temporary variable
        // that's constructed for this returns-receiver method isn't present
        // in the MC store, but is tracked by the MCIC. Therefore, the system
        // assumes the worst case (that the return type of this method is
        // the default must-call type for a SocketChannel), and issues an error.
        // TODO: fix this problem!
        sock.configureBlocking(false);
        sock.socket().setSoLinger(false, -1);
        sock.socket().setTcpNoDelay(true);
        return sock;
    }
}
