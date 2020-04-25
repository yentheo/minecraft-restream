package one.spectra.minecraft.restream;

import net.minecraft.text.Text;

public interface OnChatMessageHandler {
    public void op(Text author, Text message, Platform platform);
}