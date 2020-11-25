// Simple tests of @MustCallChoice functionality on wrapper streams.

import java.io.*;
import java.net.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.*;

class MustCallChoiceExamples {

    void test_two_locals(String address) {
        Socket socket = null;
        try {
            socket = new Socket( address, 80 );
            DataInputStream d = new DataInputStream(socket.getInputStream());
        } catch (IOException e){

        } finally {
            closeSocket(socket);
        }
    }

    void test_close_wrapper(@Owning InputStream b) throws IOException {
        DataInputStream d = new DataInputStream(b);
        d.close();
    }

    void test_close_nonwrapper(@Owning InputStream b) throws IOException {
        DataInputStream d = new DataInputStream(b);
        b.close();
    }

    void test_no_close(@Owning InputStream b) {
        // :: error: required.method.not.called
        DataInputStream d = new DataInputStream(b);
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    void closeSocket(Socket sock) {
        try {
            if(sock!=null){
                sock.close();
            }
        } catch (IOException e) {

        }
    }
}
