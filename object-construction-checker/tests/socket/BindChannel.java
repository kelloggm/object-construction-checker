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
            // The mustcall.not.parseable error could go away, if the MustCall Checker were
            // aware of which expressions will eventually get temporaries. For now, I think
            // it's okay to keep issuing this error; see the second method in this class
            // for an example of how to rewrite the code to avoid the parse error.
            //
            // :: error: required.method.not.called
            ServerSocketChannel httpChannel = ServerSocketChannel.open();
            // :: error: mustcall.not.parseable :: error: reset.not.owning
            httpChannel.socket().bind(addr);
        } catch (IOException io) {

        }
    }

    static void test_lv(InetSocketAddress addr, boolean b) {
        try {
            // :: error: required.method.not.called
            ServerSocketChannel httpChannel = ServerSocketChannel.open();
            ServerSocket httpSock = httpChannel.socket();
            httpSock.bind(addr);
        } catch (IOException io) {

        }
    }
}
