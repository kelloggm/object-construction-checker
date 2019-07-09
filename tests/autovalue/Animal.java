import com.google.auto.value.AutoValue;
import org.checkerframework.checker.builder.qual.*;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Adapted from the standard AutoValue example code:
 * https://github.com/google/auto/blob/master/value/userguide/builders.md
 */
@AutoValue
abstract class Animal {
  abstract String name();

  abstract @Nullable String habitat();

  abstract int numberOfLegs();

  public String getStr() {
    return "str";
  }

  // TEMPORARY will fix in follow-up PR
  //public abstract Builder toBuilder();

  static Builder builder() {
    return new AutoValue_Animal.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setName(String value);

    abstract Builder setNumberOfLegs(int value);

    abstract Builder setHabitat(String value);

    abstract Animal build();
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

  public static void buildSomethingRightIncludeOptional() {
    Builder b = builder();
    b.setName("Frank");
    b.setNumberOfLegs(4);
    b.setHabitat("jungle");
    b.build();
  }

  public static void buildSomethingWrongFluent() {
    // :: error: method.invocation.invalid
    builder().setName("Frank").build();
  }

  public static void buildSomethingRightFluent() {
    builder().setName("Jim").setNumberOfLegs(7).build();
  }

  // TEMPORARY will fix in follow-up PR
//  public static void buildWithToBuilder() {
//    Animal a1 = builder().setName("Jim").setNumberOfLegs(7).build();
//    a1.toBuilder().build();
//  }
}
