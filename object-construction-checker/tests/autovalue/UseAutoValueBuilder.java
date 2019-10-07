import testlib.autovalue.AVTest;

class UseAutoValueBuilder {

    void test() {
        // :: error: finalizer.invocation.invalid
        AVTest v = AVTest.builder().build();

        AVTest v2 = AVTest.builder().setName("name").build();

    }
}