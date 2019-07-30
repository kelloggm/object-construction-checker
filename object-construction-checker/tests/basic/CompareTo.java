// A test based on a false positive in a case study. It doesn't actually have anything to
// do with Comparable.

public class CompareTo {

    enum CompareToType {
        TYPE1,
        TYPE2
    }

    public int foo(CompareToType t1, CompareToType t2) {
        t1.compareTo(t2);
        t1.compareTo(t2);
    }
}