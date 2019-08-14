import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.checker.returnsrcvr.qual.*;

class Generics {

    private <T extends Symbol> T getMember(Class<T> type) {
        T sym = getMember(type);
        if (sym != null && sym.isStatic()) {
            return sym;
        }
        return null;
    }

    static interface Symbol {

        boolean isStatic();

    }
}