// Based on some false positives I found in ZK.

import java.util.List;
import java.net.Socket;
import java.io.IOException;

import org.checkerframework.checker.objectconstruction.qual.Owning;

class EnhancedFor {
    void test(List<Socket> list) {
        for (Socket s : list) {
            try {
                s.close();
            } catch (IOException i) { }
        }
    }

    void test2(List<Socket> list) {
        for (int i = 0; i < list.size(); i++) {
            Socket s = list.get(i);
            try {
                s.close();
            } catch (IOException io) { }
        }
    }
}
