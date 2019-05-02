package tests;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

public class EC2Test extends CheckerFrameworkPerDirectoryTest {
    public EC2Test(List<File> testFiles) {
        super(testFiles,
                org.checkerframework.checker.builder.TypesafeBuilderChecker.class,
                "cve",
                "-Anomsgtext",
                "-Astubs=stubs",
                "-nowarn");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"cve"};
    }
}
