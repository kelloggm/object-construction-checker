import com.google.auto.value.AutoValue;
import org.checkerframework.checker.builder.qual.*;

@AutoValue
abstract class Animal {
  abstract String name();
  abstract int numberOfLegs();

  static Builder builder() {
    return new AutoValue_Animal.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder setName(String value);
    abstract Builder setNumberOfLegs(int value);
    abstract Animal build(@CalledMethods({"setName", "setNumberOfLegs"}) Builder this);
  }

//  public static void buildSomethingWrong() {
//    Builder b = builder();
//    b.setName("Frank");
//    b.build();
//  }

//  public static void buildSomethingWrongFluent() {
//    builder().setName("Frank").build();
//  }
}