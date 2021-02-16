// Based on a false positive in hdfs

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import java.net.*;
import java.io.*;
import java.io.FilterInputStream;
import java.util.*;
import javax.net.ssl.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.nio.channels.ServerSocketChannel;
import java.lang.StringBuilder;

class HdfsReport3
{
    private StringBuffer nonObligationTest(int id) {
        final StringWriter out = new StringWriter();
        dumpTreeRecursively(new PrintWriter(out, true), new StringBuilder(),
                id);
        return out.getBuffer();
    }

    public void dumpTreeRecursively(PrintWriter out, StringBuilder prefix,
                                    int snapshotId) {
    }

    // StringBuilder doesn't implement closeable
    final private StringBuilder sb = new StringBuilder();
    final private Formatter formatter = new Formatter(sb);

}
