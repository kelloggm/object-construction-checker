// Based on an FP in ZK. Only #parse threw an error;
// removing either if statement made the code verifiable.

import java.io.*;

class DoubleIf {

    String fn;

    public void parse(boolean b, boolean c) throws Exception {
        if (c) {
            FileInputStream fis = new FileInputStream(fn);
            try {
            } finally {
                fis.close();
            }
            if (b) {
            }
        }
    }

    public void parse2(boolean c) throws Exception {
        if (c) {
            FileInputStream fis = new FileInputStream(fn);
            try {
            } finally {
                fis.close();
            }
        }
    }

    public void parse3(boolean b) throws Exception {
        FileInputStream fis = new FileInputStream(fn);
        try {
        } finally {
            fis.close();
        }
        if (b) {
        }
    }
}
