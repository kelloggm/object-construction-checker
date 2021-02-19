package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.objectconstruction.ObjectConstructionChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class NoResourceAliasesTest extends CheckerFrameworkPerDirectoryTest {
  public NoResourceAliasesTest(List<File> testFiles) {
    super(
        testFiles,
        ObjectConstructionChecker.class,
        "noresourcealias",
        "-Anomsgtext",
        "-AcheckMustCall",
        "-AnoResourceAliases",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"noresourcealias"};
  }
}
