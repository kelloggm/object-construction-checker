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
            // in the store, I think, and be replaced by a required.method.not.called error. See the
            // version below, where the bound socket is extracted into a local variable.
            //
            ServerSocketChannel httpChannel = ServerSocketChannel.open();
            // :: error: mustcall.not.parseable
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
