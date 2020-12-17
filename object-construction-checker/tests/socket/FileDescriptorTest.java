import java.io.*;
import java.io.IOException;

import java.net.*;
import org.checkerframework.checker.objectconstruction.qual.*;

public class FileDescriptorTest
{
    public static void readPropertiesFile(File from) throws IOException {
        RandomAccessFile file = new RandomAccessFile(from, "rws");
        FileInputStream in = null;
        try {
            in = new FileInputStream(file.getFD());
            file.seek(0);
        } finally {
            if (in != null) {
                in.close();
            }
            file.close();
        }
    }

    public static void sameScenario_noFD(@Owning Socket sock) throws IOException {
        InputStream in = null;
        try {
            in = sock.getInputStream();
        } finally {
            if (in != null) {
                in.close();
            }
            sock.close();
        }
    }
}
