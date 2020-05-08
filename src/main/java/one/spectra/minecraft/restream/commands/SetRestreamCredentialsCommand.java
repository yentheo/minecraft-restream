package one.spectra.minecraft.restream.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.server.command.ServerCommandSource;
import one.spectra.minecraft.restream.configuration.ConfigurationManager;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class SetRestreamCredentialsCommand {
    private ConfigurationManager configurationManager;

    public SetRestreamCredentialsCommand(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> argumentBuilder = literal("setrestreamcredentials")
            .then(argument("client_id", string()).then(argument("client_secret", string())
            .executes(context -> {
                String clientId = StringArgumentType.getString(context, "client_id");
                String clientSecret = StringArgumentType.getString(context, "client_secret");
                this.configurationManager.saveCredentials(clientId, clientSecret);
                return 1;
            })));
        return dispatcher.register(argumentBuilder);
    }
}