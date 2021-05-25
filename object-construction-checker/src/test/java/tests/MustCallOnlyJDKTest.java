package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.objectconstruction.ObjectConstructionChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class MustCallOnlyJDKTest extends CheckerFrameworkPerDirectoryTest {
  public MustCallOnlyJDKTest(List<File> testFiles) {
    super(
        testFiles,
        ObjectConstructionChecker.class,
        "mustcall",
        "-Anomsgtext",
        "-AcheckMustCall",
        "-AcountMustCall",
        "-AonlyUses=^java\\.",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"mustcall-onlyjdk"};
  }
}
