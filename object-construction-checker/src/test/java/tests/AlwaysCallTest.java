package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class AlwaysCallTest extends CheckerFrameworkPerDirectoryTest {
  public AlwaysCallTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.class,
        "alwayscall",
        "-Anomsgtext",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"alwayscall"};
  }
}
