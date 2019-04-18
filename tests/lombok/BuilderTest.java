import lombok.Builder;
import lombok.NonNull;

@Builder
public class BuilderTest {

    private Integer x;

    @NonNull
    private Integer y;

    @NonNull
    @Builder.Default
    private Integer z = 5;


    public static void test_simplePattern() {

        BuilderTest.builder().x(0).y(0).build(); // good builder

        BuilderTest.builder().y(0).build(); // good builder

        BuilderTest.builder().y(0).z(5).build();  // good builder

        // :: error: (lombok.builder.nonnull)
        BuilderTest.builder().x(0).build(); // bad builder
    }

    public static void test_builderVar() {
        final BuilderTest.BuilderTestBuilder goodBuilder = new BuilderTestBuilder();
        goodBuilder.x(0);
        goodBuilder.y(0);
        goodBuilder.build();

        final BuilderTest.BuilderTestBuilder badBuilder = new BuilderTestBuilder();
        // :: error: (lombok.builder.nonnull)
        badBuilder.build();
    }
}
