package one.spectra.minecraft.restream;

import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.spectra.minecraft.restream.commands.AuthorizeResponseResolver;
import one.spectra.minecraft.restream.configuration.ConfigurationManager;
import one.spectra.minecraft.restream.configuration.RestreamConfiguration;
import one.spectra.minecraft.restream.handlers.DisconnectHandler;
import one.spectra.minecraft.restream.restream.RestreamClient;
import one.spectra.minecraft.restream.restream.models.AuthorizeResponse;
import one.spectra.minecraft.restream.restream.models.Platform;

@Singleton
public class ConnectionManager {
    private RestreamClient restreamClient;
    private ConfigurationManager configurationManager;

    HashMap<Platform, Formatting> platformFormatting = new HashMap<Platform, Formatting>();

    HashMap<String, DisconnectHandler> connectedNames = new HashMap<String, DisconnectHandler>();

    @Inject
    public ConnectionManager(RestreamClient restreamClient, ConfigurationManager configurationManager) {
        this.restreamClient = restreamClient;
        this.configurationManager = configurationManager;
        
        platformFormatting.put(Platform.TWITCH, Formatting.DARK_PURPLE);
        platformFormatting.put(Platform.YOUTUBE, Formatting.DARK_RED);
        platformFormatting.put(Platform.UNKNOWN, Formatting.WHITE);
    }

    private boolean validateRestreamConfiguration() {
        RestreamConfiguration configuration = this.configurationManager.getConfiguration();
        return configuration.clientId != null && configuration.clientSecret != null;
    }

    public int startRestream(ServerCommandSource source, AuthorizeResponseResolver authorizeResponseResolver) {
        boolean restreamConfigurationIsValid = validateRestreamConfiguration();
        if (!restreamConfigurationIsValid) {
            source.sendError(new LiteralText("Restream is not configured. Missing client credentials."));
            return 0;
        }
        String name = source.getName();
        if (connectedNames.containsKey(name)) {
            source.sendFeedback(new LiteralText("Connected to Restream.").formatted(Formatting.GRAY), false);
            return 1;
        }
        AuthorizeResponse authorizeResponse = authorizeResponseResolver.get();
        if (authorizeResponse != null) {
            if (authorizeResponse.refresh_token != null) {
                configurationManager.saveRefreshToken(name, authorizeResponse.refresh_token);
            }
            if (authorizeResponse.access_token != null) {
                restreamClient.startListen(authorizeResponse.access_token, disconnectHandler -> {
                    source.sendFeedback(new LiteralText("Connected to Restream").formatted(Formatting.DARK_GREEN),
                            false);
                    connectedNames.put(name, disconnectHandler);
                }, (author, message, platform) -> {
                    System.out.print("message parsed");
                    Text host = new LiteralText(" (" + name.substring(0, 1).toLowerCase() + ")")
                            .formatted(platformFormatting.get(platform));
                    Text text = new LiteralText("").append(author).append(host)
                            .append(new LiteralText(": ").formatted(Formatting.WHITE)).append(message);
                    source.getMinecraftServer().getPlayerManager().broadcastChatMessage(text, true);
                }, () -> {
                    connectedNames.remove(name);
                });

            }
        }
        return 1;
    }

    public void stopRestream(ServerCommandSource source) {
        String name = source.getName();
        DisconnectHandler disconnectHandler = connectedNames.get(name);
        if (disconnectHandler != null) {
            disconnectHandler.disconnect();
        }
        connectedNames.remove(name);
    }
}