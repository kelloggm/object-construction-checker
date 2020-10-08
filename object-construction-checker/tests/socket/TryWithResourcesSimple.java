// A simple test that a try-with-resources socket doesn't issue a false positive.

import java.net.Socket;

class TryWithResourcesSimple {
    static void test(String address, int port) {
        try (Socket socket = new Socket(address, port)) {

        } catch (Exception e) {

        }
    }
}
