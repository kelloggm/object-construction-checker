package testlib;

import lombok.Builder;
import lombok.NonNull;

@Builder
public class Foo {
    @NonNull
    String requiredProperty;
}
