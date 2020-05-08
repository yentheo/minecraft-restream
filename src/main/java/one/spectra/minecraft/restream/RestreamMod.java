package one.spectra.minecraft.restream;

import net.fabricmc.api.ModInitializer;
import one.spectra.minecraft.restream.configuration.*;
import one.spectra.minecraft.restream.restream.*;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RestreamMod implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "spectra-restream";
    public static final String MOD_NAME = "spectra-restream";
    ConfigurationManager configurationManager = new ConfigurationManager();
    RestreamClient restreamClient = new RestreamClient(configurationManager);

    List<String> connectedNames = new ArrayList<String>();

    HashMap<Platform, Formatting> platformFormatting = new HashMap<Platform, Formatting>();

    @Override
    public void onInitialize() {
        platformFormatting.put(Platform.TWITCH, Formatting.DARK_PURPLE);
        platformFormatting.put(Platform.YOUTUBE, Formatting.DARK_RED);
        platformFormatting.put(Platform.UNKNOWN, Formatting.WHITE);

        LOGGER.info("Initialize Restream Mod");

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            LOGGER.info("Callback command registration");
            dispatcher.register(literal("setrestreamcredentials")
                    .then(argument("client_id", string()).then(argument("client_secret", string()).executes(context -> {
                        String clientId = StringArgumentType.getString(context, "client_id");
                        String clientSecret = StringArgumentType.getString(context, "client_secret");
                        configurationManager.saveCredentials(clientId, clientSecret);
                        return 1;
                    }))));
            dispatcher.register(literal("restream").then(argument("code", greedyString()).executes(ctx -> {
                String code = StringArgumentType.getString(ctx, "code");
                String name = ctx.getSource().getName();
                if (connectedNames.contains(name)) {
                    ctx.getSource().sendFeedback(new LiteralText("Already connected.").formatted(Formatting.GRAY),
                            false);
                    return 1;
                }
                System.out.println("Getting tokens for " + ctx.getSource().getName());
                AuthorizeResponse authorizeResponse = restreamClient.authorize(code);
                if (authorizeResponse != null) {
                    if (authorizeResponse.access_token != null) {
                        if (authorizeResponse.refresh_token != null) {
                            configurationManager.saveRefreshToken(name, authorizeResponse.refresh_token);
                            ctx.getSource().sendFeedback(
                                    new LiteralText("Saved refresh token.").formatted(Formatting.GRAY), false);
                        }
                        restreamClient.startListen(authorizeResponse.access_token, () -> {
                            ctx.getSource().sendFeedback(
                                    new LiteralText("Connected to Restream").formatted(Formatting.DARK_GREEN), false);
                            connectedNames.add(name);
                        }, (author, message, platform) -> {
                            Text host = new LiteralText("(" + name.substring(0, 1).toLowerCase() + ")")
                                    .formatted(platformFormatting.get(platform));
                            Text text = new LiteralText("").append(author).append(host)
                                    .append(new LiteralText(": ").formatted(Formatting.WHITE)).append(message);
                            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(text, true);
                        }, () -> {
                            connectedNames.remove(name);
                        });
                    } else {
                        ctx.getSource()
                                .sendFeedback(new LiteralText("Couldn't authorize, make sure the code is correct.")
                                        .formatted(Formatting.RED), false);
                    }
                } else {
                    ctx.getSource().sendFeedback(new LiteralText("Couldn't authorize, make sure the code is correct.")
                            .formatted(Formatting.RED), false);
                }
                return 1;
            })).executes(ctx -> {
                String name = ctx.getSource().getName();
                if (connectedNames.contains(name)) {
                    ctx.getSource().sendFeedback(new LiteralText("Already connected.").formatted(Formatting.GRAY),
                            false);
                    return 1;
                }
                AuthorizeResponse authorizeResponse = restreamClient.refreshAuthorizationFor(name);
                if (authorizeResponse != null) {
                    if (authorizeResponse.refresh_token != null) {
                        configurationManager.saveRefreshToken(name, authorizeResponse.refresh_token);
                        ctx.getSource().sendFeedback(new LiteralText("Saved refresh token.").formatted(Formatting.GRAY),
                                false);
                    }
                    if (authorizeResponse.access_token != null) {
                        restreamClient.startListen(authorizeResponse.access_token, () -> {
                            ctx.getSource().sendFeedback(
                                    new LiteralText("Connected to Restream").formatted(Formatting.DARK_GREEN), false);
                            connectedNames.add(name);
                        }, (author, message, platform) -> {
                            System.out.print("message parsed");
                            Text host = new LiteralText(" (" + name.substring(0, 1).toLowerCase() + ")")
                                    .formatted(platformFormatting.get(platform));
                            Text text = new LiteralText("").append(author).append(host)
                                    .append(new LiteralText(": ").formatted(Formatting.WHITE)).append(message);
                            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(text, true);
                        }, () -> {
                            connectedNames.remove(name);
                        });

                    } else {
                        ctx.getSource()
                                .sendFeedback(new LiteralText("Couldn't authorize, try re-authorizing with a code")
                                        .formatted(Formatting.RED), false);
                    }
                } else {
                    ctx.getSource().sendFeedback(new LiteralText("Couldn't authorize, try re-authorizing with a code")
                            .formatted(Formatting.RED), false);
                    System.out.println("no refresh token for current user");
                }
                return 1;
            }));
        });
    }
}