import java.io.*;
import java.io.IOException;

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
}