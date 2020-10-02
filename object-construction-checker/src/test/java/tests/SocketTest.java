package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

public class SocketTest extends CheckerFrameworkPerDirectoryTest {
  public SocketTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.class,
        "socket",
        "-Anomsgtext",
        //        "-Astubs=stubs",
        "-AuseValueChecker",
        "-nowarn");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"socket"};
  }
}
