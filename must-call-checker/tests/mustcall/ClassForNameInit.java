// Based on a number of false positives in Zookeeper that all use this pattern to reflectively
// initialize a class. -AresolveReflection fixes this version, but most (4/5) of the failures in Zookeeper
// persist even with that flag, which also imposes about a 50% perf overhead. So these are now expected warnings.

import java.io.*;

class ClassForNameInit {

    public static InputStream inputStreamFactory() throws Exception {
        // FYI this code will always fail if you run it, so don't.
        // There's no ByteArrayInputStream constructor that takes no arguments.
        Class<?> baisClass = Class.forName("java.io.ByteArrayInputStream");
        Object bais = baisClass.getConstructor().newInstance();
        return (InputStream) bais;
    }

    public static Object objectFactory() throws Exception {
        Class<?> objClass = Class.forName("java.lang.Object");
        Object obj = objClass.getConstructor().newInstance();
        // :: error: return.type.incompatible
        return (Object) obj;
    }
}
