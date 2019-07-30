// This test is an attempt to elicit the same kind of error exhibited in a case
// study by code like what's in CompareTo.java, but dispensing with the enum.
// Apparently, the enum is required, because this test issued no errors.

public class InnerClassCompareTo {

    class CompareToType {
        int value;
    }

    public int foo(CompareToType t1, CompareToType t2) {
        if (t1.value - t2.value != 0) {
            return t1.value - t2.value;
        }
        return 0;
    }

    class CompareToType2 {
        int compare(CompareToType2 other) {
            return 5;
        }
    }

    public int foo2(CompareToType2 t1, CompareToType2 t2) {
        if (t1.compare(t2) != 0) {
            return t1.compare(t2);
        }
        return 0;
    }
}