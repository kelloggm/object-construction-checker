// A test that some custom error reporting mechanisms respect the -AonlyUses command line option.

import java.io.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import java.nio.ByteBuffer;

class OnlyJDK {
    @MustCall("a")
    class Foo {
        void a() {}
    }

    void test_custom() {
        // normally, a required.method.not.called error would be issued here
        Foo foo = new Foo();
    }

    void test_jdk(String fn) throws FileNotFoundException {
        // :: error: required.method.not.called
        new FileInputStream(fn);
    }

    // This class copied from tests/mustcall/ZookeeperByteBufferInputStream.java
    @MustCall({})
    // normally, an inconsistent.mustcall.subtype error would be issued here
    class ZookeeperByteBufferInputStream extends InputStream {

        ByteBuffer bb;

        // :: error: super.invocation.invalid
        public ZookeeperByteBufferInputStream(ByteBuffer bb) {
            this.bb = bb;
        }

        @Override
        public int read() throws IOException {
            if (bb.remaining() == 0) {
                return -1;
            }
            return bb.get() & 0xff;
        }

        @Override
        public int available() throws IOException {
            return bb.remaining();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (bb.remaining() == 0) {
                return -1;
            }
            if (len > bb.remaining()) {
                len = bb.remaining();
            }
            bb.get(b, off, len);
            return len;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public long skip(long n) throws IOException {
            if (n < 0L) {
                return 0;
            }
            n = Math.min(n, bb.remaining());
            bb.position(bb.position() + (int) n);
            return n;
        }
    }
}
