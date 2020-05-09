package one.spectra.minecraft.restream.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;
import one.spectra.minecraft.restream.Platform;
import one.spectra.minecraft.restream.configuration.ConfigurationManager;
import one.spectra.minecraft.restream.configuration.RestreamConfiguration;
import one.spectra.minecraft.restream.restream.AuthorizeResponse;
import one.spectra.minecraft.restream.restream.RestreamClient;

import static net.minecraft.server.command.CommandManager.literal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static net.minecraft.server.command.CommandManager.argument;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class RestreamCommand {

    private RestreamClient restreamClient;
    private ConfigurationManager configurationManager;

    HashMap<Platform, Formatting> platformFormatting = new HashMap<Platform, Formatting>();

    List<String> connectedNames = new ArrayList<String>();

    @Inject
    public RestreamCommand(RestreamClient restreamClient, ConfigurationManager configurationManager) {
        this.restreamClient = restreamClient;
        this.configurationManager = configurationManager;
    }

    private boolean validateRestreamConfiguration() {
        RestreamConfiguration configuration = this.configurationManager.getConfiguration();
        return configuration.clientId != null && configuration.clientSecret != null;
    }

    public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> argumentBuilder = literal("restream")
                .then(argument("code", greedyString()).executes(context -> startRestream(context, () -> {
                    AuthorizeResponse authorizeResponse = restreamClient
                            .authorize(StringArgumentType.getString(context, "code"));
                    return authorizeResponse;
                }))).executes(context -> startRestream(context, () -> {
                    AuthorizeResponse authorizeResponse = restreamClient
                            .refreshAuthorizationFor(context.getSource().getName());
                    if (authorizeResponse == null) {
                        LiteralText message = new LiteralText(
                                "Couldn't authorize, try reauthorizing with a code by visiting: ");
                        context.getSource().sendError(message.append(generateLinkText()));
                    }
                    return authorizeResponse;
                }));
        return dispatcher.register(argumentBuilder);
    }

    private LiteralText generateLinkText() {
        HashMap<String, String> queryStringParameters = new HashMap<String, String>();
        queryStringParameters.put("response_type", "code");
        queryStringParameters.put("client_id", "033a362e-10fe-4a6d-8c6f-3f55f99584d3");
        queryStringParameters.put("redirect_uri", "http://localhost");
        queryStringParameters.put("state", generateRandomString());
        String baseUrl = "https://api.restream.io/login";
        String queryString = queryStringParameters.entrySet().stream().map(x -> x.getKey() + '=' + x.getValue())
                .reduce((previous, current) -> previous + "&" + current).orElse("");
        String fullUrl = baseUrl + "?" + queryString;
        LiteralText link = new LiteralText("Restream");
        System.out.println(fullUrl);
        ClickEvent clickEvent = new ClickEvent(Action.OPEN_URL, fullUrl);
        Style linkStyle = link.getStyle();
        Style newStyle = linkStyle.withFormatting(Formatting.UNDERLINE).withClickEvent(clickEvent);
        link.setStyle(newStyle);
        return link;
    }

    private String generateRandomString() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        return IntStream.rangeClosed(1, 8).mapToObj(i -> chars)
                .map(c -> c.charAt(new Random().nextInt(chars.length() - 1))).map(c -> c.toString())
                .reduce("", (previous, current) -> previous + current);
    }

    private int startRestream(CommandContext<ServerCommandSource> context,
            AuthorizeResponseResolver authorizeResponseResolver) {
        boolean restreamConfigurationIsValid = validateRestreamConfiguration();
        if (!restreamConfigurationIsValid) {
            context.getSource().sendError(new LiteralText("Restream is not configured. Missing client credentials."));
            return 0;
        }
        String name = context.getSource().getName();
        if (connectedNames.contains(name)) {
            context.getSource().sendFeedback(new LiteralText("Already connected.").formatted(Formatting.GRAY), false);
            return 1;
        }
        AuthorizeResponse authorizeResponse = authorizeResponseResolver.get();
        if (authorizeResponse != null) {
            if (authorizeResponse.refresh_token != null) {
                configurationManager.saveRefreshToken(name, authorizeResponse.refresh_token);
                context.getSource().sendFeedback(new LiteralText("Saved refresh token.").formatted(Formatting.GRAY),
                        false);
            }
            if (authorizeResponse.access_token != null) {
                restreamClient.startListen(authorizeResponse.access_token, () -> {
                    context.getSource().sendFeedback(
                            new LiteralText("Connected to Restream").formatted(Formatting.DARK_GREEN), false);
                    connectedNames.add(name);
                }, (author, message, platform) -> {
                    System.out.print("message parsed");
                    Text host = new LiteralText(" (" + name.substring(0, 1).toLowerCase() + ")")
                            .formatted(platformFormatting.get(platform));
                    Text text = new LiteralText("").append(author).append(host)
                            .append(new LiteralText(": ").formatted(Formatting.WHITE)).append(message);
                    context.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(text, true);
                }, () -> {
                    connectedNames.remove(name);
                });

            }
        }
        return 1;
    }
}