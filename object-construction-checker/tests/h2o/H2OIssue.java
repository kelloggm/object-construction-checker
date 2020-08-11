//import org.checkerframework.checker.objectconstruction.qual.*;
//
//
//class H2OIssue {
//
//    abstract private class ModelBuilder<M extends Model<M, P, O>, P extends Model.Parameters, O extends Model.Output> extends Iced {
//
//
//        public static <B extends ModelBuilder, MP extends Model.Parameters> B make(String algo, Job job, Key<Model> result) {
//            B newB = ModelBuilder.m(String algo, Job job, Key<Model> result);
//            return newB;
//        }
//
//        public <B extends ModelBuilder> B m(String algo, Job job, Key<Model> result) {
//            return null;
//        }
//    }
//
//
//    private class Iced<D extends Iced> {
//    }
//
//    private class Job<T extends Keyed> extends Keyed<Job> {
//        public Job(Key<T> key, String clz_of_T, String desc) {
//        }
//    }
//    private class Keyed<T extends Keyed> extends Iced<T> {}
//    private class Key<T extends Keyed> extends Iced<Key<T>> {
//    }
//
//    private class Model<M extends Model<M, P, O>, P extends Model.Parameters, O extends Model.Output> {
//
//        private class Parameters extends Iced<Parameters> {
//        }
//
//        private class Output extends Iced {
//        }
//
//    }
//
//
//
//
//}