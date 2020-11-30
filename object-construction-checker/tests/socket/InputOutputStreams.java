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

    // getInputStream()/getOutputStream() can throw IOException in three different scenarios:
    // 1) The underlying socket is already closed
    // 2) The underlying socket is not connected
    // 3) The underlysing socket input is shutdown
    // In the first case our checker always reports a false positive but for the second case
    // and third case our checker has to verify that close is called on the underlying resource.
    // So, because sock.getInputStream() can throw IOException, "is" can be null, then sock will
    // remain open. So, it's a true pasitive warning.
    // :: error: required.method.not.called
    void test_close_is(@Owning Socket sock) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = sock.getInputStream();
            os = sock.getOutputStream();
        } catch (IOException e) {

        } finally {
            is.close();
        }
    }

    // :: error: required.method.not.called
    void test_close_os(@Owning Socket sock) throws IOException {
        InputStream is = sock.getInputStream();
        OutputStream os = sock.getOutputStream();
        os.close();
    }

    // :: error: required.method.not.called
    void test_close_os2(@Owning Socket sock) throws IOException {
        OutputStream os = null;
        InputStream is = null;
        try {
            is = sock.getInputStream();
        } catch (IOException e) {
            try {
            os = sock.getOutputStream();
            } catch (IOException ee) {}
        } finally {
            os.close();
        }
    }

    // :: error: required.method.not.called
    void test_close_os3(@Owning Socket sock) throws IOException {
        OutputStream os = null;
        try {
            InputStream is = sock.getInputStream();
        } catch (IOException e) { }
        try {
            os = sock.getOutputStream();
        } finally {
            os.close();
        }
    }

    //TODO pass this case
    void test_close_os4(@Owning Socket sock) throws IOException {
        OutputStream os = null;
        try {
            InputStream is = sock.getInputStream();
        } catch (IOException e) { }
        try {
            os = sock.getOutputStream();
        } finally {
            if (os != null) {
                os.close();
            } else {
                sock.close();
            }
        }
    }

    // :: error: required.method.not.called
    void test_close_buff(@Owning Socket sock) throws IOException {
        BufferedOutputStream buff = null;
        try {
            InputStream is = sock.getInputStream();
            OutputStream os = sock.getOutputStream();
            buff = new BufferedOutputStream(os);
        } catch (IOException e) {

        } finally {
            buff.close();
        }
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
