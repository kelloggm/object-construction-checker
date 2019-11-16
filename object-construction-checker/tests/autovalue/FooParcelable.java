import com.google.auto.value.AutoValue;
import android.os.Parcelable;

/**
 * Test for support of AutoValue Parcel extension
 */
@AutoValue
abstract class FooParcelable implements Parcelable {
  abstract String name();

  static Builder builder() {
    return new AutoValue_FooParcelable.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setName(String value);

    abstract FooParcelable build();
  }

  public static void buildSomethingWrong() {
    Builder b = builder();
    // :: error: finalizer.invocation.invalid
    b.build();
  }

  public static void buildSomethingRight() {
    Builder b = builder();
    b.setName("Frank");
    b.build();
  }

}
