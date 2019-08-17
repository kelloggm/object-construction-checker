import com.google.auto.value.AutoValue;
import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class SetInsideBuild {
    public abstract String name();
    public abstract int size();
    static Builder builder() {
        return new AutoValue_SetInsideBuild.Builder();
    }
    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder setName(String name);
        abstract Builder setSize(int value);
        abstract SetInsideBuild autoBuild();
        public SetInsideBuild build(Builder this) {
            // :: error: finalizer.invocation.invalid
            return this.setSize(4).autoBuild(); // error here
        }
    }
}