package one.spectra.minecraft.restream.restream.handlers;

import net.minecraft.text.Text;
import one.spectra.minecraft.restream.restream.models.Platform;

public interface OnChatMessageHandler {
    public void op(Text author, Text message, Platform platform);
}