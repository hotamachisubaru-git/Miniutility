package org.hotamachisubaru.miniutility.listeners.renderer;

import net.kyori.adventure.text.Component;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;

/** ローダー固有のチャットイベントに依存しない表示フォーマット。 */
public final class NicknameChatRenderer {
    private NicknameChatRenderer() { }
    public static Component render(Component name, Component message) {
        return LegacyTextFormatter.chatMessage(name, message);
    }
}
