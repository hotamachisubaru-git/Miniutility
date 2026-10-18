package org.hotamachisubaru.miniutility.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class ModText {
    private ModText() { }
    public static Component legacy(String text) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder part = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            ChatFormatting format = (c == '&' || c == '§') && i + 1 < text.length()
                    ? ChatFormatting.getByCode(Character.toLowerCase(text.charAt(i + 1))) : null;
            if (format != null) {
                if (part.length() > 0) { result.append(Component.literal(part.toString()).setStyle(style)); part.setLength(0); }
                style = format == ChatFormatting.RESET ? Style.EMPTY
                        : "0123456789abcdef".indexOf(Character.toLowerCase(text.charAt(i + 1))) >= 0 ? Style.EMPTY.applyFormat(format) : style.applyFormat(format);
                i++;
            } else part.append(c);
        }
        if (part.length() > 0) result.append(Component.literal(part.toString()).setStyle(style));
        return result;
    }
}
