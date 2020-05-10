package one.spectra.minecraft.restream.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import one.spectra.minecraft.restream.ConnectionManager;
import one.spectra.minecraft.restream.LinkTextGenerator;
import one.spectra.minecraft.restream.restream.RestreamClient;
import one.spectra.minecraft.restream.restream.models.AuthorizeResponse;

import static net.minecraft.server.command.CommandManager.literal;

import static net.minecraft.server.command.CommandManager.argument;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class RestreamCommand {

    private RestreamClient restreamClient;
    private ConnectionManager connectionManager;

    @Inject
    public RestreamCommand(RestreamClient restreamClient, ConnectionManager connectionManager) {
        this.restreamClient = restreamClient;
        this.connectionManager = connectionManager;
    }

    public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> argumentBuilder = literal("restream")
                .then(argument("code", greedyString())
                        .executes(context -> connectionManager.startRestream(context.getSource(), () -> {
                            AuthorizeResponse authorizeResponse = restreamClient
                                    .authorize(StringArgumentType.getString(context, "code"));
                            return authorizeResponse;
                        })))
                .executes(context -> connectionManager.startRestream(context.getSource(), () -> {
                    AuthorizeResponse authorizeResponse = restreamClient
                            .refreshAuthorizationFor(context.getSource().getName());
                    if (authorizeResponse == null) {
                        LiteralText message = new LiteralText(
                                "Couldn't authorize, try reauthorizing with a code by visiting: ");

                        String url = LinkTextGenerator.generateLink();
                        Text linkText = LinkTextGenerator.generateLinkText("Restream", url);
                        context.getSource().sendError(message.append(linkText));
                    }
                    return authorizeResponse;
                }));
        return dispatcher.register(argumentBuilder);
    }

}