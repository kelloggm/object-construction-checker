import java.nio.channels.SocketChannel;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;

class EnsuresCalledMethodsIfTest {

    @EnsuresCalledMethods(value = "#1", methods = "close")
    public static void closeSock(SocketChannel sock) {
        if (!sock.isOpen()) {
            return;
        }
        try {
            sock.close();
        } catch (Exception e) {
        }
    }
}
