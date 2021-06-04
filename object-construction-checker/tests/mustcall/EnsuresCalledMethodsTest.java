import java.io.IOException;
import java.net.ServerSocket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.objectconstruction.qual.Owning;

public class EnsuresCalledMethodsTest {

    private @Owning ServerSocket ss;

    @EnsuresCalledMethods(value = "ss", methods = "close")
    public synchronized void stop() {
        if (ss != null) {
            try {
                ss.close();
            } catch (IOException e) {
            }
        }
    }
}
