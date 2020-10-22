// A test for a class that wraps a map. I found a similar example in Zookeeper that causes false
// positives.

import java.util.HashMap;

class MapWrap<E> {
    HashMap<E, String> impl = new HashMap<E, String>();

    String remove(E e) {
        String old = impl.remove(e);
        return old;
    }
}
