import testlib.autovalue.AVTest;

class UseAutoValueBuilder {

    void test() {
        AVTest v = AVTest.builder().build();

        AVTest v2 = AVTest.builder().setName("name").build();

    }
}