package one.spectra.minecraft.restream.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import one.spectra.minecraft.restream.ConnectionManager;
import one.spectra.minecraft.restream.IoCModule;
import one.spectra.minecraft.restream.LinkTextGenerator;
import one.spectra.minecraft.restream.restream.RestreamClient;
import one.spectra.minecraft.restream.restream.models.AuthorizeResponse;

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
                String url = LinkTextGenerator.generateLink();
                Text linkText = LinkTextGenerator.generateLinkText("Restream", url);
                player.getCommandSource().sendError(message.append(linkText));
            }
            return authorizeResponse;
        });
    }
}