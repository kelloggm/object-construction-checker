import org.checkerframework.checker.objectconstruction.qual.*;


class H2OIssue {

    class ModelBuilder<M extends Model<M, P, O>, P extends Model.Parameters, O extends Model.Output> extends Iced {

        /**
         * All the parameters required to build the model.
         */
        public P _parms;              // Not final, so CV can set-after-clone

        /**
         * All the parameters required to build the model conserved in the input form, with AUTO values not evaluated yet.
         */
        public P _input_parms;


        /**
         * Unique new job and named result key
         */
        public ModelBuilder(P parms, Key<Model> key) {
            _parms = parms;
            _input_parms = parms;
        }


        public static <B extends ModelBuilder, P extends Model.Parameters> B make(P parms, Key<Model> mKey) {
            B newMB = new ModelBuilder<>(parms, mKey);
            newMB._parms = parms;
            newMB._input_parms = parms;
            return newMB;
        }
    }


        private class Keyed<T extends Keyed> extends Iced<T> {
        }

    private class Iced<D extends Iced> {
    }

    private class Key<T extends Keyed> extends Iced<Key<T>> {
    }

    private class Model<M extends Model<M, P, O>, P extends Model.Parameters, O extends Model.Output> {

        private class Parameters extends Iced<Parameters> {
        }

        private class Output extends Iced {
        }

    }




}