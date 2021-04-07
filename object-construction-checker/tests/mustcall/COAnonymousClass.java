// Test case for https://github.com/kelloggm/object-construction-checker/issues/368

import org.checkerframework.checker.mustcall.qual.*;

class COAnonymousClass {
    static class Foo {

        @CreatesObligation("this")
        void resetFoo() { }

        void other() {

            Runnable r = new Runnable() {
                @Override
                // :: error: creates.obligation.override.invalid
                @CreatesObligation
                public void run() {
                    resetFoo();
                }
            };
            other2(r);
        }

        // If this call to run() were permitted with no errors, this would be unsound.
        void other2(Runnable r) { r.run(); }
    }
}
