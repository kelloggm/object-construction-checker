import com.google.inject.Injector;

public class Main {

    public static void main(String[] args) {
        final Injector injector = com.google.inject.Guice.createInjector(new Module());
        final Main guiceObject = injector.getInstance(Main.class);
    }

}
