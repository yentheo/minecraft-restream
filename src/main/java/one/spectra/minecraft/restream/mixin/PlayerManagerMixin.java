package one.spectra.minecraft.restream.mixin;

import java.util.HashMap;
import java.util.Random;
import java.util.stream.IntStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;
import one.spectra.minecraft.restream.ConnectionManager;
import one.spectra.minecraft.restream.IoCModule;
import one.spectra.minecraft.restream.restream.AuthorizeResponse;
import one.spectra.minecraft.restream.restream.RestreamClient;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void connectToRestream(ClientConnection clientConnection, ServerPlayerEntity player, CallbackInfo ci) {
        ConnectionManager connectionManager = IoCModule.INJECTOR.getInstance(ConnectionManager.class);
        RestreamClient restreamClient = IoCModule.INJECTOR.getInstance(RestreamClient.class);
        connectionManager.startRestream(player.getCommandSource(), () -> {
            AuthorizeResponse authorizeResponse = restreamClient
                    .refreshAuthorizationFor(player.getCommandSource().getName());
            if (authorizeResponse == null) {
                LiteralText message = new LiteralText(
                        "Couldn't authorize, try reauthorizing with a code by visiting: ");
                player.getCommandSource().sendError(message.append(generateLinkText()));
            }
            return authorizeResponse;
        });
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
}