// A test for code encountered by Narges.

import java.nio.channels.*;
import java.io.*;
import java.net.*;

class BindChannel {
    static void test(InetSocketAddress addr, boolean b) {
        try {
            // This channel is bound - so even with unconnected socket support, we need to
            // treat either this channel or the .socket() expression as must-close.
            //
            // The mustcall.not.parseable error should go away when we have support for expressions
            // in the store, I think.
            //
            // :: error: required.method.not.called
            ServerSocketChannel httpChannel = ServerSocketChannel.open();
            // :: error: mustcall.not.parseable
            httpChannel.socket().bind(addr);
        } catch (IOException io) {

        }
    }
}
