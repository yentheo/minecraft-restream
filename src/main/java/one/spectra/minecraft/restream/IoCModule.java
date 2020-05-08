package one.spectra.minecraft.restream;

import com.google.inject.AbstractModule;

import one.spectra.minecraft.restream.commands.RestreamCommand;
import one.spectra.minecraft.restream.commands.SetRestreamCredentialsCommand;
import one.spectra.minecraft.restream.configuration.ConfigurationManager;
import one.spectra.minecraft.restream.restream.RestreamClient;

public class IoCModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RestreamClient.class).to(RestreamClient.class);
        bind(ConfigurationManager.class).to(ConfigurationManager.class);

        bind(RestreamCommand.class).to(RestreamCommand.class);
        bind(SetRestreamCredentialsCommand.class).to(SetRestreamCredentialsCommand.class);
    }
    
}