package org.hotamachisubaru.miniutility.Listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;

import java.util.Objects;

public final class ChatPaperListener implements Listener {

    private static final Component CHAT_SEPARATOR = Component.text(" » ", NamedTextColor.GRAY);

    private final Chat chatListener;
    private final NicknameManager nicknameManager;

    public ChatPaperListener(Chat chatListener, NicknameManager nicknameManager) {
        this.chatListener = Objects.requireNonNull(chatListener, "chatListener");
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChatEvent(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (chatListener.tryHandleWaitingInput(player, Chat.toPlainText(event.message()))) {
            event.setCancelled(true);
            return;
        }

        Component display = nicknameManager.buildDisplayComponent(player);
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
                display.append(CHAT_SEPARATOR).append(message)
        ));
    }
}
