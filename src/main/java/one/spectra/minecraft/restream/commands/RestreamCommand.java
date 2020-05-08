package one.spectra.minecraft.restream.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.spectra.minecraft.restream.Platform;
import one.spectra.minecraft.restream.configuration.ConfigurationManager;
import one.spectra.minecraft.restream.restream.AuthorizeResponse;
import one.spectra.minecraft.restream.restream.RestreamClient;

import static net.minecraft.server.command.CommandManager.literal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> argumentBuilder = literal("restream")
                .then(argument("code", greedyString()).executes(context -> startRestream(context,
                        () -> restreamClient.authorize(StringArgumentType.getString(context, "code")))))
                .executes(context -> startRestream(context,
                        () -> restreamClient.refreshAuthorizationFor(context.getSource().getName())));
        return dispatcher.register(argumentBuilder);
    }

    private int startRestream(CommandContext<ServerCommandSource> context,
            AuthorizeResponseResolver authorizeResponseResolver) {
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

            } else {
                context.getSource().sendFeedback(
                        new LiteralText("Couldn't authorize, try re-authorizing with a code").formatted(Formatting.RED),
                        false);
            }
        } else {
            context.getSource().sendFeedback(
                    new LiteralText("Couldn't authorize, try re-authorizing with a code").formatted(Formatting.RED),
                    false);
            System.out.println("no refresh token for current user");
        }
        return 1;
    }
}