package one.spectra.minecraft.restream;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class IoCModule extends AbstractModule {

    public static Injector INJECTOR = Guice.createInjector(new IoCModule());

    @Override
    protected void configure() {
    }

}