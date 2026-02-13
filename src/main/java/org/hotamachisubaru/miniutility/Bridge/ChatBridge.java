package org.hotamachisubaru.miniutility.Bridge;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.hotamachisubaru.miniutility.Listener.Chat;

import java.util.Objects;

public final class ChatBridge implements Listener {

    private final Chat chatListener;

    public ChatBridge(Chat chatListener) {
        this.chatListener = Objects.requireNonNull(chatListener, "chatListener");
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        if (chatListener.tryHandleWaitingInput(player, Chat.toPlainText(e.message()))) {
            e.setCancelled(true);
        }
    }
}
