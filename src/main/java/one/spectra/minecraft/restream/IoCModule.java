package one.spectra.minecraft.restream;

import com.google.inject.AbstractModule;

import one.spectra.minecraft.restream.configuration.ConfigurationManager;
import one.spectra.minecraft.restream.restream.RestreamClient;

public class IoCModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RestreamClient.class).to(RestreamClient.class);
        bind(ConfigurationManager.class).to(ConfigurationManager.class);
    }
    
}