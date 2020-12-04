// A simple subtyping test for the unconnected sockets hierarchy

import org.checkerframework.checker.unconnectedsocket.qual.*;

class Subtyping {
    void test_top(@PossiblyConnected Object obj) {
        @PossiblyConnected Object t = obj;
        // :: error: assignment.type.incompatible
        @Unconnected Object b = obj;
    }

    void test_bot(@Unconnected Object obj) {
        @PossiblyConnected Object t = obj;
        @Unconnected Object b = obj;
    }

    // default is @PossiblyConnected
    void test_default(Object obj) {
        @PossiblyConnected Object t = obj;
        // :: error: assignment.type.incompatible
        @Unconnected Object b = obj;
    }
}
