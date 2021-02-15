// Based on a false positive in hdfs

import java.io.*;

public class HdfsReport1 extends FilterOutputStream {
    public HdfsReport1(File f) throws FileNotFoundException {
        // It's a false positive
        // :: error: required.method.not.called
        super(new FileOutputStream(new File(f.getParentFile(), f.getName() + "String")));
    }
}