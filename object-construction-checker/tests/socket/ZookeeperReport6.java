// Based on a Zookeeper false positive that requires unconnected socket support.

import java.nio.channels.SocketChannel;
import java.io.IOException;

class ZookeeperReport6 {
    SocketChannel createSock() throws IOException {
        SocketChannel sock;
        // :: error: required.method.not.called
        sock = SocketChannel.open();
        sock.configureBlocking(false);
        sock.socket().setSoLinger(false, -1);
        sock.socket().setTcpNoDelay(true);
        return sock;
    }
}
