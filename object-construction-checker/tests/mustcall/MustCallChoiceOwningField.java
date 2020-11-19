// Based on a MustCallChoice scenario in Zookeeper.

import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import java.io.*;

public @MustCall("shutdown") class MustCallChoiceOwningField {

    private @Owning BufferedInputStream input;

    public MustCallChoiceOwningField(@Owning BufferedInputStream input, boolean b) {
        this.input = input;
        if (b) {
            authenticate(new DataInputStream(input));
        }
    }

    @EnsuresCalledMethods(value="this.input", methods="close")
    public void shutdown() throws IOException {
        input.close();
    }

    public static void authenticate(InputStream is) {

    }
}
