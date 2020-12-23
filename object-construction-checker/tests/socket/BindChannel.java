// A test for a false positive encountered by Narges.

import java.nio.channels.*;
import java.io.*;
import java.net.*;

class BindChannel {
    static void test(InetSocketAddress addr, boolean b) {
        try {
            ServerSocketChannel httpChannel = ServerSocketChannel.open();
            httpChannel.socket().bind(addr);
        } catch (IOException io) {

        }
    }
}
