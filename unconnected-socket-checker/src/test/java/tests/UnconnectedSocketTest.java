package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class UnconnectedSocketTest extends CheckerFrameworkPerDirectoryTest {
  public UnconnectedSocketTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.unconnectedsocket.UnconnectedSocketChecker.class,
        "unconnectedsocket",
        "-Anomsgtext",
        // "-AstubDebug");
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"unconnectedsocket"};
  }
}
