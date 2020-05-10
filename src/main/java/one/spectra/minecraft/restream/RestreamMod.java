package one.spectra.minecraft.restream;

import net.fabricmc.api.ModInitializer;
import one.spectra.minecraft.restream.commands.RestreamCommand;
import one.spectra.minecraft.restream.commands.SetRestreamCredentialsCommand;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RestreamMod implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "spectra-restream";
    public static final String MOD_NAME = "spectra-restream";

    @Override
    public void onInitialize() {
        RestreamCommand restreamCommand = IoCModule.INJECTOR.getInstance(RestreamCommand.class);
        SetRestreamCredentialsCommand setRestreamCredentialsCommand = IoCModule.INJECTOR.getInstance(SetRestreamCredentialsCommand.class);

        LOGGER.info("Initialize Restream Mod");
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            restreamCommand.register(dispatcher);
            setRestreamCredentialsCommand.register(dispatcher);
        });
    }
}