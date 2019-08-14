import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.returnsrcvr.qual.*;

class Generics {

    private <T extends Symbol> T getMember(Class<T> type, boolean b) {
        if (b) {
            T sym = getMember(type, !b);
            if (sym != null && sym.isStatic()) {
                return sym;
            }
        } else {
            T sym = getMember(type, b);
              if (sym != null) {
                return sym;
              }
        }
        return null;
    }

    static interface Symbol {

        boolean isStatic();

    }
}