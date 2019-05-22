import com.google.auto.value.AutoValue;
import org.checkerframework.checker.builder.qual.*;

/**
 * Adapted from the standard AutoValue example code:
 * https://github.com/google/auto/blob/master/value/userguide/builders.md
 */
@AutoValue
abstract class UnannotAnimal {
  abstract String name();

  abstract int numberOfLegs();

  static Builder builder() {
    return new AutoValue_UnannotAnimal.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setName(String value);

    abstract Builder setNumberOfLegs(int value);

    abstract UnannotAnimal build();
  }

  public static void buildSomethingWrong() {
    Builder b = builder();
    b.setName("Frank");
    // :: error: method.invocation.invalid
    b.build();
  }

  public static void buildSomethingRight() {
    Builder b = builder();
    b.setName("Frank");
    b.setNumberOfLegs(4);
    b.build();
  }

  public static void buildSomethingWrongFluent() {
    // :: error: method.invocation.invalid
    builder().setName("Frank").build();
  }

  public static void buildSomethingRightFluent() {
    builder().setName("Jim").setNumberOfLegs(7).build();
  }
}