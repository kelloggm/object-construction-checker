import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        install(new OtherModule()); // the "install" can be used to get additional @Provides from other classes
    }

    /*
     * This is the problematic method: the method requires a String and a Double to be provided, but the module
     * only provides a String (the Double is commented out). If the method has an argument type for which we do
     * not have a @Provides, it fails at runtime.
     * Note that there are other ways of getting Provided parameters such as "@Named"
     */
    @Provides
    public Main providesGuiceObject(final String thisExists,
                                    final Double thisDoesntExist,
                                    final Float comesFromElsewhere) {
        return new Main();
    }

    @Provides
    private String privedesSomeString() {
        return "Hello World";
    }

    /*
        If this provider is commented in, there is no runtime error anymore.
     */
//    @Provides
//    private Double privedesSomeDouble() {
//        return 1.0;
//    }

}
