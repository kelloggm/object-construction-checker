package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.objectconstruction.ObjectConstructionChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

public class CountTest extends CheckerFrameworkPerDirectoryTest {
  public CountTest(List<File> testFiles) {
    super(
        testFiles,
        ObjectConstructionChecker.class,
        "counter",
        "-Anomsgtext",
        //        "-Astubs=stubs",
        "-AuseValueChecker",
        "-AcheckMustCall",
        "-AcountMustCall",
        "-nowarn");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"counter"};
  }
}
