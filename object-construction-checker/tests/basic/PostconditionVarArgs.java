import org.checkerframework.checker.objectconstruction.qual.*;

/** Test for postcondition support via @EnsureCalledMethods */
class PostconditionVarArgs {
  void build(@CalledMethods({"b", "c"}) PostconditionVarArgs this) {}

  void b() {}

  void c() {}

  // :: error: ensuresvarargs.unverified
  @EnsuresCalledMethodsVarArgs("b")
  static void callB(PostconditionVarArgs... x) {
    for (PostconditionVarArgs p : x) {
      p.b();
    }
  }

  @EnsuresCalledMethodsVarArgs("b")
  // :: error: ensuresvarargs.annotation.invalid
  static void callBBadAnnot(PostconditionVarArgs x) {
    x.b();
  }

  // :: error: ensuresvarargs.unverified
  @EnsuresCalledMethodsVarArgs({"b", "c"})
  static void callBAndC(PostconditionVarArgs... x) {
    for (PostconditionVarArgs p : x) {
      p.b();
      p.c();
    }
  }


  static void invokeCallB() {
    PostconditionVarArgs y = new PostconditionVarArgs();
    PostconditionVarArgs z = new PostconditionVarArgs();
    callB(y, z);
    y.c();
    z.c();
    y.build();
    z.build();
  }

  static void invokeCallBLast() {
    PostconditionVarArgs y = new PostconditionVarArgs();
    PostconditionVarArgs z = new PostconditionVarArgs();
    y.c();
    z.c();
    callB(y, z);
    y.build();
    z.build();
  }

  static void invokeCallBAndC() {
    PostconditionVarArgs y = new PostconditionVarArgs();
    PostconditionVarArgs z = new PostconditionVarArgs();
    callBAndC(y, z);
    y.build();
    z.build();
  }

  static void invokeCallBAndCArray() {
    PostconditionVarArgs y = new PostconditionVarArgs();
    PostconditionVarArgs z = new PostconditionVarArgs();
    PostconditionVarArgs[] argsArray = {y, z};
    callBAndC(argsArray);
    // because we don't handle arrays
    // :: error: finalizer.invocation.invalid
    y.build();
  }

  static void invokeCallBWrong() {
    PostconditionVarArgs y = new PostconditionVarArgs();
    callB(y);
    // :: error: finalizer.invocation.invalid
    y.build();
  }
}
