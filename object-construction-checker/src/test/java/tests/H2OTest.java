package tests;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

public class H2OTest extends CheckerFrameworkPerDirectoryTest {
    public H2OTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.class,
                "h2o",
                "-Anomsgtext",
                "-nowarn");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"h2o"};
    }
}