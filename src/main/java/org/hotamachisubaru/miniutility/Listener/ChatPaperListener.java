package org.hotamachisubaru.miniutility.Listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;

import java.util.Objects;

public final class ChatPaperListener implements Listener {

    private static final String CHAT_SEPARATOR = " \u00A77\u00BB ";

    private final Chat chatListener;
    private final NicknameManager nicknameManager;

    public ChatPaperListener(Chat chatListener, NicknameManager nicknameManager) {
        this.chatListener = Objects.requireNonNull(chatListener, "chatListener");
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChatEvent(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (chatListener.tryHandleWaitingInput(player, event.getMessage())) {
            event.setCancelled(true);
            return;
        }

        String display = nicknameManager.buildDisplayLegacy(player);
        if (display == null || display.isBlank()) {
            display = player.getName();
        }

        // AsyncPlayerChatEvent#setFormat uses Formatter syntax. Escape '%' to avoid format errors.
        event.setFormat(escapeForFormat(display) + CHAT_SEPARATOR + "%2$s");
    }

    private static String escapeForFormat(String value) {
        return value.replace("%", "%%");
    }
}
