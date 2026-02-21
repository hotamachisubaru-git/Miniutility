package org.hotamachisubaru.miniutility.Bridge;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.hotamachisubaru.miniutility.Listener.Chat;

import java.util.Objects;

public final class ChatBridge implements Listener {

    private final Chat chatListener;

    public ChatBridge(Chat chatListener) {
        this.chatListener = Objects.requireNonNull(chatListener, "chatListener");
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (chatListener.tryHandleWaitingInput(player, e.getMessage())) {
            e.setCancelled(true);
        }
    }
}
