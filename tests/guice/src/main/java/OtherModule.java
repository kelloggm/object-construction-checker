import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class OtherModule extends AbstractModule {

    @Override
    protected void configure() {
        // do nothing
    }

    @Provides
    public Float providesFloat() {
        return 1.0f;
    }
}
