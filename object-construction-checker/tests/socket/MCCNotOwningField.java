// A test case that a MustCallAlias value with a non-owning field with a must-call obligation
// does not lead to a false positive.

import java.net.Socket;

class MCCNotOwningField {

    final Socket s;

    MCCNotOwningField(Socket s) throws Exception {
        this.s = s;
    }

    void simple() throws Exception {
        s.getInputStream();
    }
}
