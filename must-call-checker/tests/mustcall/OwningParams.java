// Tests that parameters (including receiver parameters) marked as @Owning are still checked.

import org.checkerframework.checker.objectconstruction.qual.Owning;
import org.checkerframework.checker.mustcall.qual.MustCall;

class OwningParams {
    static void o1(@Owning OwningParams o) { }

    void o2(@Owning OwningParams this) { }

    void test(@MustCall({"a"}) OwningParams o, OwningParams p) {
        // :: error: argument.type.incompatible
        o1(o);
        // TODO: this error doesn't show up! See MustCallVisitor#skipReceiverSubtypeCheck
        //  error: method.invocation.invalid
        o.o2();
        o1(p);
        p.o2();
    }
}
