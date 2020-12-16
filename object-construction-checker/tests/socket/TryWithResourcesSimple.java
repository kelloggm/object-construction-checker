// A simple test that a try-with-resources socket doesn't issue a false positive.

import java.net.Socket;
import java.io.*;
import java.util.*;
import java.nio.channels.*;

class TryWithResourcesSimple {
    static void test(String address, int port) {
        try (Socket socket = new Socket(address, port)) {

        } catch (Exception e) {

        }
    }

    public static void hadoopReport(File to, Properties props)
            throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(to, "rws");
             FileOutputStream outSpecial = new FileOutputStream(file.getFD())) {
            file.seek(0);
            /*
             * If server is interrupted before this line,
             * the version file will remain unchanged.
             */
            props.store(outSpecial, null);
            /*
             * Now the new fields are flushed to the head of the file, but file
             * length can still be larger then required and therefore the file can
             * contain whole or corrupted fields from its old contents in the end.
             * If server is interrupted here and restarted later these extra fields
             * either should not effect server behavior or should be handled
             * by the server correctly.
             */
            file.setLength(outSpecial.getChannel().position());
        }
    }

    public boolean isPreUpgradableLayout(File oldF) throws IOException {

        if (!oldF.exists()) {
            return false;
        }
        // check the layout version inside the storage file
        // Lock and Read old storage file
        try (RandomAccessFile oldFile = new RandomAccessFile(oldF, "rws");
             FileLock oldLock = oldFile.getChannel().tryLock()) {
            if (null == oldLock) {
                throw new OverlappingFileLockException();
            }
            oldFile.seek(0);
            int oldVersion = oldFile.readInt();
            return false;
        }
    }
}
