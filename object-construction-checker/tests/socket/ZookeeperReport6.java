// Based on a Zookeeper false positive that requires unconnected socket support.

// @skip-test until unconnected socket support is fixed

import java.nio.channels.SocketChannel;
import java.io.IOException;

class ZookeeperReport6 {
    SocketChannel createSock() throws IOException {
        SocketChannel sock;
        sock = SocketChannel.open();
        sock.configureBlocking(false);
        sock.socket().setSoLinger(false, -1);
        sock.socket().setTcpNoDelay(true);
        return sock;
    }
}
