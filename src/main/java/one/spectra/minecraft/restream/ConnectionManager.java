package one.spectra.minecraft.restream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.spectra.minecraft.restream.commands.AuthorizeResponseResolver;
import one.spectra.minecraft.restream.configuration.ConfigurationManager;
import one.spectra.minecraft.restream.configuration.RestreamConfiguration;
import one.spectra.minecraft.restream.restream.RestreamClient;
import one.spectra.minecraft.restream.restream.models.AuthorizeResponse;
import one.spectra.minecraft.restream.restream.models.Platform;

@Singleton
public class ConnectionManager {
    private RestreamClient restreamClient;
    private ConfigurationManager configurationManager;

    HashMap<Platform, Formatting> platformFormatting = new HashMap<Platform, Formatting>();

    List<String> connectedNames = new ArrayList<String>();
    
    @Inject
    public ConnectionManager(RestreamClient restreamClient, ConfigurationManager configurationManager) {
        this.restreamClient = restreamClient;
        this.configurationManager = configurationManager;
    }

    private boolean validateRestreamConfiguration() {
        RestreamConfiguration configuration = this.configurationManager.getConfiguration();
        return configuration.clientId != null && configuration.clientSecret != null;
    }
    
    public int startRestream(ServerCommandSource source,
            AuthorizeResponseResolver authorizeResponseResolver) {
        boolean restreamConfigurationIsValid = validateRestreamConfiguration();
        if (!restreamConfigurationIsValid) {
            source.sendError(new LiteralText("Restream is not configured. Missing client credentials."));
            return 0;
        }
        String name = source.getName();
        if (connectedNames.contains(name)) {
            source.sendFeedback(new LiteralText("Connected to Restream.").formatted(Formatting.GRAY), false);
            return 1;
        }
        AuthorizeResponse authorizeResponse = authorizeResponseResolver.get();
        if (authorizeResponse != null) {
            if (authorizeResponse.refresh_token != null) {
                configurationManager.saveRefreshToken(name, authorizeResponse.refresh_token);
                source.sendFeedback(new LiteralText("Saved refresh token.").formatted(Formatting.GRAY),
                        false);
            }
            if (authorizeResponse.access_token != null) {
                restreamClient.startListen(authorizeResponse.access_token, () -> {
                    source.sendFeedback(
                            new LiteralText("Connected to Restream").formatted(Formatting.DARK_GREEN), false);
                    connectedNames.add(name);
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
}