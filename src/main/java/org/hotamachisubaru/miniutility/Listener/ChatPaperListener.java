package org.hotamachisubaru.miniutility.Listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.util.LuckPermsUtil;

public final class ChatPaperListener implements Listener {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChatEvent(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plain = Chat.toPlainText(event.message());
        if (Chat.tryHandleWaitingInput(player, plain)) {
            event.setCancelled(true);
            return;
        }

        String prefix = "";
        try {
            prefix = LuckPermsUtil.safePrefix(player);
        } catch (Throwable ignored) {
        }

        String nickname = NicknameManager.getDisplayName(player);
        if (nickname == null || nickname.isEmpty()) nickname = player.getName();
        if (!prefix.isEmpty() && nickname.startsWith(prefix)) prefix = "";

        final Component display = toLegacyComponent(
                prefix.isEmpty() ? nickname : (prefix + "&r " + nickname)
        );

        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
                Component.empty()
                        .append(display)
                        .append(Component.text(" » "))
                        .append(message)
        ));
    }

    private static Component toLegacyComponent(String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) return Component.empty();
        return LEGACY_SERIALIZER.deserialize(legacyText.replace('§', '&'));
    }
}
