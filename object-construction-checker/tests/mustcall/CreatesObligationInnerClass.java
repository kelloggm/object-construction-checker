// Test case for https://github.com/kelloggm/object-construction-checker/issues/368

import org.checkerframework.checker.mustcall.qual.*;

class CreatesObligationInnerClass {
    static class Foo {

        @CreatesObligation("this")
        void resetFoo() { }

        void other() {

            Runnable r = new Runnable() {
                @Override
                @CreatesObligation
                // :: error: creates.obligation.override.invalid
                public void run() {
                    resetFoo();
                }
            };
            other2(r);
        }

        // If this call to run() were permitted with no errors, this would be unsound.
        void other2(Runnable r) { r.run(); }

        /**
         * non-static inner class
         */
        class Bar {

            // this should be disallowed! not sure of the right error message
            // :: error: creates.obligation.override.invalid
            @CreatesObligation
            void bar() {
                resetFoo();
            }
        }

        void callBar() {
            Bar b = new Bar();
            // If this call to bar() were permitted with no errors, this would be unsound.
            b.bar();
        }
    }
}
