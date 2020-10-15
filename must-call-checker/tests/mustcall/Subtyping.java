// simple subtyping test for the MustCall annotation

import org.checkerframework.checker.mustcall.qual.*;

class Subtyping {

    Object unannotatedObj;

    void test_act(@MustCallTop Object o) {
        @MustCallTop Object act = o;
        // :: error: assignment.type.incompatible
        @MustCall("close") Object file = o;
        // :: error: assignment.type.incompatible
        @MustCall({"close", "read"}) Object f2 = o;
        // :: error: assignment.type.incompatible
        @MustCall({}) Object notAfile = o;
        // :: error: assignment.type.incompatible
        unannotatedObj = o;
    }

    void test_close(@MustCall("close") Object o) {
        @MustCallTop Object act = o;
        @MustCall("close") Object file = o;
        @MustCall({"close", "read"}) Object f2 = o;
        // :: error: assignment.type.incompatible
        @MustCall({}) Object notAfile = o;
        // :: error: assignment.type.incompatible
        unannotatedObj = o;
    }

    void test_close_read(@MustCall({"close", "read"}) Object o) {
        @MustCallTop Object act = o;
        // :: error: assignment.type.incompatible
        @MustCall("close") Object file = o;
        @MustCall({"close", "read"}) Object f2 = o;
        // :: error: assignment.type.incompatible
        @MustCall({}) Object notAfile = o;
        // :: error: assignment.type.incompatible
        unannotatedObj = o;
    }

    void test_blank(@MustCall({}) Object o) {
        @MustCallTop Object act = o;
        @MustCall("close") Object file = o;
        @MustCall({"close", "read"}) Object f2 = o;
        @MustCall({}) Object notAfile = o;
        unannotatedObj = o;
    }

    void test_unannotated(Object o) {
        @MustCallTop Object act = o;
        @MustCall("close") Object file = o;
        @MustCall({"close", "read"}) Object f2 = o;
        @MustCall({}) Object notAfile = o;
        unannotatedObj = o;
    }
}
