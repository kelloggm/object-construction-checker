package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.objectconstruction.ObjectConstructionChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class MustCallTest extends CheckerFrameworkPerDirectoryTest {
  public MustCallTest(List<File> testFiles) {
    super(
        testFiles,
        ObjectConstructionChecker.class,
        "mustcall",
        "-Anomsgtext",
        "-AcheckMustCall",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"mustcall"};
  }
}
