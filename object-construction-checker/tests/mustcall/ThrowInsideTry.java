// Based on an FP in ZK.

import java.io.*;

class ThrowInsideTry {

    public static class MyException extends Exception {
    }

    public static void test(boolean b) {
        try {
            FileInputStream f = new FileInputStream("foo.txt");
            try {
                if (b) {
                    throw new MyException();
                }
            } finally {
                f.close();
            }
        } catch (Exception e) {

        }
    }
}
