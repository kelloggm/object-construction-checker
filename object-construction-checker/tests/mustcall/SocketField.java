// test case for https://github.com/kelloggm/object-construction-checker/issues/381

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;

import java.net.Socket;
import java.io.IOException;

@MustCall("closeSocket")
class SocketField {
    protected @Owning Socket socket = null;

    @CreatesObligation("this")
    protected void setupConnection(javax.net.SocketFactory socketFactory) throws IOException {
        this.socket.close();
        //this.socket = new Socket();
        @CalledMethods("close") Socket s = this.socket;
        this.socket = socketFactory.createSocket();
    }
    @EnsuresCalledMethods(value = "this.socket", methods = "close")
    private void closeSocket() throws IOException {
        this.socket.close();
    }
}
