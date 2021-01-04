// @skip-test until the checks are implemented
import java.net.*;
import java.nio.channels.*;
import java.io.IOException;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;

class HdfsReport2 {
    void sequenceInTernaryExp(boolean b) throws IOException{
        ServerSocket ss = (b) ?
                ServerSocketChannel.open().socket() : new ServerSocket();

        ss.close();
    }

    void sequenceInTernaryExp2(boolean b) throws IOException{
        ServerSocketChannel x = ServerSocketChannel.open();
        ServerSocket y = x.socket();
        ServerSocket z = new ServerSocket();
        ServerSocket ss = (b) ?
                y : z;

        ss.close();
    }

    FileLock lock;
    public void unlock() throws IOException {
        if (this.lock == null)
            return;
        this.lock.release();
        lock.channel().close();
    }

    public void isLockSupported(@Owning FileLock firstLock, @Owning FileLock secondLock) throws IOException {

        if(firstLock != null && firstLock != lock) {
            firstLock.release();
            firstLock.channel().close();
        }
        if(secondLock != null) {
            secondLock.release();
            secondLock.channel().close();
        }
    }

}


