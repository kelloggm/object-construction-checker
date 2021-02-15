// Based on a false positive in hdfs

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

import java.io.*;
import java.nio.file.*;
import java.lang.*;

public class HdfsReport2 {
    static void readCheckpointTime(File timeFile) throws IOException {
        // false positive
        // :: error: required.method.not.called
        DataInputStream in = new DataInputStream(Files.newInputStream(timeFile.toPath()));
        try {
            in.close();
            in = null;
        } finally {
            cleanupWithLogger(in);
        }
    }

    @SuppressWarnings("ensuresvarargs.unverified")
    @EnsuresCalledMethodsVarArgs("close")
    public static void cleanupWithLogger(java.io.Closeable... closeables) {
        for (java.io.Closeable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable e) {
                }
            }
        }
    }
}