package testlib.autovalue;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AVTest {

    public abstract String name();

    public static Builder builder() {
        return new AutoValue_AVTest.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setName(String value);

        public abstract AVTest build();
    }
}
