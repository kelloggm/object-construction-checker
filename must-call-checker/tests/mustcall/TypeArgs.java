import org.checkerframework.checker.mustcall.qual.MustCall;

public class TypeArgs {

    static class A<Q extends @MustCall({"carly"}) Object> {}

    // :: error: (type.argument.type.incompatible)
    static class B<S> extends A<S> {}

    public <T> void f1(Generic<T> real, Generic<? super T> other, boolean flag) {
        // :: error: (type.argument.type.incompatible)
        f2(flag ? real : other);
    }

    <@MustCall({"carly"}) Q extends @MustCall({"carly"}) Object> void f2(Generic<? extends Q> parm) {}

    interface Generic<F> {}

    void m3(
            @MustCall({}) Object a,
            @MustCall({"foo"}) Object b,
            @MustCall({"bar"}) Object c,
            @MustCall({"foo", "bar"}) Object d) {
        requireNothing1(a);
        requireNothing2(a);
        // :: error: (type.argument.type.incompatible)
        requireNothing1(b);
        // :: error: (argument.type.incompatible)
        requireNothing2(b);
        // :: error: (type.argument.type.incompatible)
        requireNothing1(c);
        // :: error: (argument.type.incompatible)
        requireNothing2(c);
        // :: error: (type.argument.type.incompatible)
        requireNothing1(d);
        // :: error: (argument.type.incompatible)
        requireNothing2(d);

        requireFoo1(a);
        requireFoo2(a);
        requireFoo1(b);
        requireFoo2(b);
        // :: error: (type.argument.type.incompatible)
        requireFoo1(c);
        // :: error: (argument.type.incompatible)
        requireFoo2(c);
        // :: error: (type.argument.type.incompatible)
        requireFoo1(d);
        // :: error: (argument.type.incompatible)
        requireFoo2(d);

        requireBar1(a);
        requireBar2(a);
        // :: error: (type.argument.type.incompatible)
        requireBar1(b);
        // :: error: (argument.type.incompatible)
        requireBar2(b);
        requireBar1(c);
        requireBar2(c);
        // :: error: (type.argument.type.incompatible)
        requireBar1(d);
        // :: error: (argument.type.incompatible)
        requireBar2(d);

        requireFooBar1(a);
        requireFooBar2(a);
        requireFooBar1(b);
        requireFooBar2(b);
        requireFooBar1(c);
        requireFooBar2(c);
        requireFooBar1(d);
        requireFooBar2(d);
    }

    public static <T extends @MustCall({}) Object> T requireNothing1(T obj) {
        return obj;
    }

    public static <T> @MustCall({}) T requireNothing2(@MustCall({}) T obj) {
        return obj;
    }

    public static <T extends @MustCall({"foo"}) Object> T requireFoo1(T obj) {
        return obj;
    }

    public static <T> @MustCall({"foo"}) T requireFoo2(@MustCall({"foo"}) T obj) {
        return obj;
    }

    public static <T extends @MustCall({"bar"}) Object> T requireBar1(T obj) {
        return obj;
    }

    public static <T> @MustCall({"bar"}) T requireBar2(@MustCall({"bar"}) T obj) {
        return obj;
    }

    public static <T extends @MustCall({"foo", "bar"}) Object> T requireFooBar1(T obj) {
        return obj;
    }

    public static <T> @MustCall({"foo", "bar"}) T requireFooBar2(@MustCall({"foo", "bar"}) T obj) {
        return obj;
    }
}
