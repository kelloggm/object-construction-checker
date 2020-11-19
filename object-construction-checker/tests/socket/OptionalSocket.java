// A test case for some false positives that came up in Zookeeper.
// These are sort-of a must call choice situation, but it's not as clear.
// I suspect our MCC implementation won't actually handle these cases, in
// which case it's fine with me to skip this test for now. - Martin

import java.util.*;
import java.net.*;
import java.io.*;
import org.checkerframework.checker.objectconstruction.qual.*;

class OptionalSocket {
    void test_close_get_null(@Owning Optional<Socket> sock) throws IOException {
        if (sock.get() != null) {
            sock.get().close();
        }
    }

    void test_close_get(@Owning Optional<Socket> sock) throws IOException {
        sock.get().close();
    }
}