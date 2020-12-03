// Based on a Zookeeper false positive that requires unconnected socket support.

import java.net.Socket;
import java.io.IOException;
import org.checkerframework.checker.unconnectedsocket.qual.Unconnected;

class ZookeeperReport1 {

    int tickTime, initLimit;

    protected Socket createSocket() throws IOException {
        Socket sock;
        sock = new Socket();
        sock.setSoTimeout(this.tickTime * this.initLimit);
        return sock;
    }

    protected Socket createSocket2() throws IOException {
        Socket sock;
        sock = createCustomSocket();
        sock.setSoTimeout(this.tickTime * this.initLimit);
        return sock;
    }

    // This is the full version of case 1.
    protected Socket createSocket3(boolean b) throws IOException {
        Socket sock;
        if (b) {
            sock = createCustomSocket();
        } else {
            sock = new Socket();
        }
        sock.setSoTimeout(this.tickTime * this.initLimit);
        return sock;
    }

    private @Unconnected Socket createCustomSocket() {
        return new Socket();
    }
}
