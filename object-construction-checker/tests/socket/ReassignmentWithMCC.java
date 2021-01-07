import java.io.*;
import java.security.*;
import org.checkerframework.checker.objectconstruction.qual.*;

public class ReassignmentWithMCC
{
    void testReassignment(File newFile, MessageDigest digester) throws IOException {
        FileOutputStream fout = new FileOutputStream(newFile);
        DigestOutputStream fos = new DigestOutputStream(fout, digester);
        DataOutputStream out = new DataOutputStream(fos);
        try {
            out = new DataOutputStream(new BufferedOutputStream(fos));
            fout.getChannel();
        } finally {
            out.close();
        }
    }

    void testReassignmentSetSizeOne(@Owning FilterOutputStream out) throws IOException {
        out = new DataOutputStream(out);
        out.close();
    }
}