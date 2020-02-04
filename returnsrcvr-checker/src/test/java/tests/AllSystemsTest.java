package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.returnsrcvr.ReturnsRcvrChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test runner for tests of the Returns Receiver Checker.
 *
 * <p>Tests appear as Java files in the {@code tests/all-systems} folder. To add a new test case,
 * create a Java file in that directory. The file contains "// ::" comments to indicate expected
 * errors and warnings; see
 * https://github.com/typetools/checker-framework/blob/master/checker/tests/README .
 */
public class AllSystemsTest extends CheckerFrameworkPerDirectoryTest {
  public AllSystemsTest(List<File> testFiles) {
    super(
        testFiles,
        ReturnsRcvrChecker.class,
        "all-systems",
        "-Anomsgtext",
        "-Astubs=stubs/",
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"all-systems"};
  }
}
