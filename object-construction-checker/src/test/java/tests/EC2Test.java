package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.objectconstruction.ObjectConstructionChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

public class EC2Test extends CheckerFrameworkPerDirectoryTest {
  public EC2Test(List<File> testFiles) {
    super(
        testFiles,
        ObjectConstructionChecker.class,
        "cve",
        "-Anomsgtext",
        "-Astubs=stubs",
        "-AuseValueChecker",
        "-nowarn");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"cve"};
  }
}
