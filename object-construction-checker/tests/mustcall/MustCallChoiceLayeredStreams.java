// Test case based on an MCC situation in Zookeeper.

import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import java.io.*;

class MustCallChoiceLayeredStreams {

    // :: error: required.method.not.called
    @Owning InputStream cache;

    public InputStream createInputStream(File f) throws FileNotFoundException {
        if (cache == null) {
            // The real version of this uses a mix of JDK and custom streams, so it makes more sense...
            cache = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        }
        return cache;
    }
}
