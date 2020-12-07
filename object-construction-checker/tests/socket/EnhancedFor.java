// Based on some false positives I found in ZK.

import java.util.List;
import java.net.Socket;
import java.io.IOException;

class EnhancedFor {
    void test(List<Socket> list) {
        for (Socket s : list) {
            try {
                s.close();
            } catch (IOException i) { }
        }
    }
}
