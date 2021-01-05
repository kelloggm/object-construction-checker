// A test that the EnsuresCalledMethodsIf annotations in
// the Socket stub files are respected.

import org.checkerframework.checker.objectconstruction.qual.Owning;
import java.net.*;
import java.io.*;

class IsClosed {
    void test_socket(@Owning Socket sock) {
        if (!sock.isClosed()) {
            try {
                sock.close();
            } catch (IOException io) {

            }
        }
    }

    void test_server_socket(@Owning ServerSocket sock) {
        if (!sock.isClosed()) {
            try {
                sock.close();
            } catch (IOException io) {

            }
        }
    }
}
