// Taken from ACSocketTest and put here to ease debugging.

import java.net.Socket;
import java.io.IOException;

class SocketNullOverwrite {
    void replaceVarWithNull(String address, int port) {
        try {
            // :: error: required.method.not.called
            Socket s = new Socket(address, port);
            s = null;
        } catch (IOException e) {

        }
    }
}
