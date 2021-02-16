package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.objectconstruction.ObjectConstructionChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class NoAccumulationFramesTest extends CheckerFrameworkPerDirectoryTest {
  public NoAccumulationFramesTest(List<File> testFiles) {
    super(
        testFiles,
        ObjectConstructionChecker.class,
        "noaccumulationframes",
        "-Anomsgtext",
        "-AcheckMustCall",
        "-AnoAccumulationFrames",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"noaccumulationframes"};
  }
}
