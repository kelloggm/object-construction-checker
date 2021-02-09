import java.net.*;

class CrashOnCommon {
//    Object baz = new Object(new Socket());
    static void baz(Socket s) {  }
    static {
        // :: error: required.method.not.called
        baz(new Socket());
    }
}