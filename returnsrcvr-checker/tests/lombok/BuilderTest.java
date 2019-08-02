// Generated by delombok at Thu May 16 14:44:28 PDT 2019

// This only tests that it doesn't crash. I can't think of a good
// way to test that the annotations are actually applied w/o
// the CalledMethods Checker.

import lombok.NonNull;

public class BuilderTest {
    private Integer x;
    @NonNull
    private Integer y;
    @NonNull
    private Integer z;

    public static void test_simplePattern() {
        BuilderTest.builder().x(0).y(0).build(); // good builder
        BuilderTest.builder().y(0).build(); // good builder
        BuilderTest.builder().y(0).z(5).build(); // good builder
    }

    public static void test_builderVar() {
        final BuilderTest.BuilderTestBuilder goodBuilder = new BuilderTestBuilder();
        goodBuilder.x(0);
        goodBuilder.y(0);
        goodBuilder.build();
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    private static Integer $default$z() {
        return 5;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    BuilderTest(final Integer x, @NonNull final Integer y, @NonNull final Integer z) {
        if (y == null) {
            throw new java.lang.NullPointerException("y is marked non-null but is null");
        }
        if (z == null) {
            throw new java.lang.NullPointerException("z is marked non-null but is null");
        }
        this.x = x;
        this.y = y;
        this.z = z;
    }


    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public static class BuilderTestBuilder {
        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        private Integer x;
        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        private Integer y;
        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        private boolean z$set;
        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        private Integer z;

        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        BuilderTestBuilder() {
        }

        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        public BuilderTestBuilder x(final Integer x) {
            this.x = x;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        public BuilderTestBuilder y(@NonNull final Integer y) {
            if (y == null) {
                throw new java.lang.NullPointerException("y is marked non-null but is null");
            }
            this.y = y;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        public BuilderTestBuilder z(@NonNull final Integer z) {
            if (z == null) {
                throw new java.lang.NullPointerException("z is marked non-null but is null");
            }
            this.z = z;
            z$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        public BuilderTest build(BuilderTestBuilder this) {
            Integer z = this.z;
            if (!z$set) z = BuilderTest.$default$z();
            return new BuilderTest(x, y, z);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        @lombok.Generated
        public java.lang.String toString() {
            return "BuilderTest.BuilderTestBuilder(x=" + this.x + ", y=" + this.y + ", z=" + this.z + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public static BuilderTestBuilder builder() {
        return new BuilderTestBuilder();
    }
}