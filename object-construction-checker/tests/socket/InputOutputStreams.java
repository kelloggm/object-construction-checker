// A MustCallChoice test wrt sockets. Also coincidentally tests that MCC sets can be larger than two.

import java.io.*;
import java.net.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.IOException;

class InputOutputStreams {
    void test_close_sock(@Owning Socket sock) throws IOException {
        try{
            InputStream is = sock.getInputStream();
            OutputStream os = sock.getOutputStream();
        } catch (IOException e) {

        } finally {
            sock.close();
        }
    }

    void test_close_is(@Owning Socket sock) throws IOException {
        InputStream is = sock.getInputStream();
        OutputStream os = sock.getOutputStream();
        is.close();
    }

    void test_close_os(@Owning Socket sock) throws IOException {
        InputStream is = sock.getInputStream();
        OutputStream os = sock.getOutputStream();
        os.close();
    }

    void test_close_buff(@Owning Socket sock) throws IOException {
        InputStream is = sock.getInputStream();
        OutputStream os = sock.getOutputStream();
        BufferedOutputStream buff = new BufferedOutputStream(os);
        buff.close();
    }

    void test_write(String host, int port) {
        Socket sock = null;
        try {
            sock = new Socket(host, port);
            sock.getOutputStream().write("isro".getBytes());
        } catch (Exception e) {
            System.err.println("write failed: " + e);
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch (IOException e) {
                    System.err.println("couldn't close socket!");
                }
            }
        }
    }
}
