// Test case based on a false positive reported by Narges that was
// caused by not respecting ownership transfer rules for constructor params.

import java.io.*;
class BinaryInputArchive{

    private DataInput in;

    public BinaryInputArchive(DataInput in) {
        this.in = in;
    }

    public static BinaryInputArchive getArchive(InputStream strm) {
        return new BinaryInputArchive(
                new DataInputStream(
                        strm));
    }
}
