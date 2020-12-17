// A test for some issues related to the getFD() method in RandomAccessFile.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

class FileDescriptors {
    void test(@Owning RandomAccessFile r) throws Exception {
        @MustCall("close") FileDescriptor fd = r.getFD();
        // :: error: assignment.type.incompatible
        @MustCall({}) FileDescriptor fd2 = r.getFD();
    }

    void test2(@Owning RandomAccessFile r) throws Exception {
        @MustCall("close") FileInputStream f = new FileInputStream(r.getFD());
        // :: error: assignment.type.incompatible
        @MustCall({}) FileInputStream f2 = new FileInputStream(r.getFD());
    }
}
