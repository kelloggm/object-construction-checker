// A test to make sure compareTo is handled correctly.

public class CompareTo implements Comparable<CompareTo> {
    public CompareToType type;

    enum CompareToType {
        TYPE1,
        TYPE2
    }

    @Override
    public int compareTo(CompareTo other) {

        if (type.compareTo(other.type) != 0) {
            return type.compareTo(other.type);
        }

        return 0;
    }
}