import java.net.*;

class CommonModuleCrash {
    // :: error: required.method.not.called
    Socket baz = new Socket();
    static void baz(Socket s) {  }
    static {
        // :: error: required.method.not.called
        baz(new Socket());
    }
}