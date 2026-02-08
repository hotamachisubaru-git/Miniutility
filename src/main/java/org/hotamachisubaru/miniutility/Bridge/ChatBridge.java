package org.hotamachisubaru.miniutility.Bridge;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.hotamachisubaru.miniutility.Listener.Chat;

public final class ChatBridge implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncChatEvent e) {
        final Player player = e.getPlayer();
        final String plain = Chat.toPlainText(e.message());

        // 待機フラグの共通処理（消費したらキャンセル）
        if (Chat.tryHandleWaitingInput(player, plain)) {
            e.setCancelled(true);
        }
    }
}
