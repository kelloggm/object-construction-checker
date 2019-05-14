package tests;

import org.checkerframework.com.google.common.collect.ImmutableList;
import org.checkerframework.framework.test.*;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.checkerframework.framework.test.TestConfigurationBuilder.buildDefaultConfiguration;

/**
 * Test runner that uses the Checker Framework's tooling.
 *
 * <p>The test data is located in the "tests/basic" folder (by Checker Framework convention). If this test fails,
 * that means that one or more of the Java files in that directory, when run through the typechecker, did
 * not produce the expected results.
 *
 * <p>When looking at the tests in that folder, lines starting with "// :: error: " are expected errors. The test will
 * fail if they are not present. The rest of those lines are error keys, which are printed by the typechecker. So,
 * for example, "// :: error: argument.type.incompatible" means that an argument type on the following line should
 * be incompatible with a parameter type.
 *
 * <p>To add a new test case, create a Java file in that directory. Use the "// :: error: " syntax to add any expected
 * warnings. All files ending in .java in that directory will automatically be run by this test runner.
 *
 * <p>This test runner depends on the Checker Framework's testing library, which is found in the Maven artifact
 * org.checkerframework:testlib.
 */
public class AutoValueTest extends CheckerFrameworkPerDirectoryTest {

    private final ImmutableList<String> annotationProcs = ImmutableList.of(
        "com.google.auto.value.extension.memoized.processor.MemoizedValidator",
        "com.google.auto.value.processor.AutoAnnotationProcessor",
        "com.google.auto.value.processor.AutoOneOfProcessor",
        "com.google.auto.value.processor.AutoValueBuilderProcessor",
        "com.google.auto.value.processor.AutoValueProcessor",
        org.checkerframework.checker.builder.TypesafeBuilderChecker.class.getName()
    );

    public AutoValueTest(List<File> testFiles) {
        super(testFiles,
                org.checkerframework.checker.builder.TypesafeBuilderChecker.class,
                "basic",
                "-Anomsgtext",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"autovalue"};
    }

    @Override
    public void run() {
        boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
        List<String> customizedOptions =
            customizeOptions(Collections.unmodifiableList(checkerOptions));
        TestConfiguration config =
            buildDefaultConfiguration(
                testDir,
                testFiles,
                annotationProcs,
                customizedOptions,
                shouldEmitDebugInfo);
        TypecheckResult testResult = new TypecheckExecutor().runTest(config);
        TestUtilities.assertResultsAreValid(testResult);
    }
}
