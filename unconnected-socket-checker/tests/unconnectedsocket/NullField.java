// This test checks that extraneous unconnected.field errors aren't reported on
// fields that are initialized to null, which was a problem with the original
// implementation of that error.

public class NullField {
    public String foo = null;

    public String bar;
}
