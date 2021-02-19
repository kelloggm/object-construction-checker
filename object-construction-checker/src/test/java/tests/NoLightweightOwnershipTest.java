package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.objectconstruction.ObjectConstructionChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class NoLightweightOwnershipTest extends CheckerFrameworkPerDirectoryTest {
  public NoLightweightOwnershipTest(List<File> testFiles) {
    super(
        testFiles,
        ObjectConstructionChecker.class,
        "nolightweightownership",
        "-Anomsgtext",
        "-AcheckMustCall",
        "-AnoLightweightOwnership",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nolightweightownership"};
  }
}
