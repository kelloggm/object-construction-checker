package tests;

import static org.checkerframework.framework.test.TestConfigurationBuilder.buildDefaultConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.framework.test.PerDirectorySuite;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * Custom test runner that uses the Checker Framework's tooling.
 *
 * <p>The standard CF tooling isn't used because another annotation processor needs to be used.
 */
@RunWith(PerDirectorySuite.class)
public class LombokTest {

  private final String testDir = "lombok";

  private final String[] checkerOptions = {"-Anomsgtext", "-nowarn", "-Xprefer:source"};

  private final List<File> testFiles;

  private final String[] processors = {
    "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
    "org.checkerframework.checker.builder.TypesafeBuilderChecker"
  };

  public LombokTest(List<File> testFiles) {
    this.testFiles = testFiles;
  }

  @Test
  public void run() {
    boolean shouldEmitDebugInfo = true; // TestUtilities.getShouldEmitDebugInfo();
    List<String> customizedOptions = Arrays.asList(checkerOptions);
    List<String> processorNames = Arrays.asList(processors);
    TestConfiguration config =
        buildDefaultConfiguration(
            testDir, testFiles, processorNames, customizedOptions, shouldEmitDebugInfo);
    TypecheckResult testResult = new TypecheckExecutor().runTest(config);
    TestUtilities.assertResultsAreValid(testResult);
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"lombok"};
  }
}
