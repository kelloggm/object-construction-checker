// A test for a bad interaction between CO and subtyping
// that could happen if CO was unsound.

import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class COAnonymousClass {
  static class Foo {

    @CreatesObligation("this")
    void resetFoo() { }

    void other() {

      Runnable r = new Runnable() {
        @Override
        @CreatesObligation
        public void run() {
          resetFoo();
        }
      };
    }
  }
}
