// test for CO and inner classes

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
      other2(r);
    }

    void other2(Runnable r) { r.run(); }
  }
}
