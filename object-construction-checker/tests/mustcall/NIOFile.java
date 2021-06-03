// Test case that certain APIs in java.nio.file don't actually need to be closed.

import java.nio.file.WatchService;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.io.IOException;

class NIOFile {
    void test(Path dirPath) throws IOException {
        FileSystem fs = dirPath.getFileSystem();
        WatchService watchService = fs.newWatchService();
    }
}
