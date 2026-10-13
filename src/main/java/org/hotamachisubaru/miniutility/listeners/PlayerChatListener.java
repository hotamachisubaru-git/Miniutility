package org.hotamachisubaru.miniutility.listeners;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.hotamachisubaru.miniutility.listeners.input.ChatInputRouter;
import org.hotamachisubaru.miniutility.listeners.input.ChatInputMode;
import org.hotamachisubaru.miniutility.listeners.renderer.NicknameChatRenderer;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Handles Paper chat rendering and transfers GUI input to the owning server-thread session. */
public final class PlayerChatListener implements Listener {

    private final PlayerNicknameService nicknameManager;
    private final ChatInputRouter inputDispatcher;
    private final ConcurrentMap<UUID, InputSession> pendingInputs = new ConcurrentHashMap<>();

    public PlayerChatListener(PlayerNicknameService nicknameManager) {
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
        this.inputDispatcher = new ChatInputRouter(
                nicknameManager,
                nicknameManager::runOnServerThread,
                (player, message) -> player.sendMessage(LegacyTextFormatter.text(message.text(), message.color()))
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        InputSession session = pendingInputs.get(playerId);
        if (session != null) {
            String input = LegacyTextFormatter.plain(event.message()).trim();
            event.setCancelled(true);
            nicknameManager.runOnServerThread(() -> handlePendingInput(player, playerId, session, input));
            return;
        }
        NicknameChatRenderer renderer = nicknameManager.getCachedDisplayNameComponent(playerId)
                .map(NicknameChatRenderer::new)
                .orElseGet(NicknameChatRenderer::new);
        event.renderer(ChatRenderer.viewerUnaware(renderer));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        nicknameManager.updateDisplayName(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        nicknameManager.updateDisplayName(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        forgetPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        forgetPlayer(event.getPlayer());
    }

    public void awaitNicknameInput(Player player) {
        setInputMode(player, ChatInputMode.NICKNAME);
    }

    public void awaitColorNicknameInput(Player player) {
        setInputMode(player, ChatInputMode.COLORED_NICKNAME);
    }

    public void awaitExpInput(Player player) {
        setInputMode(player, ChatInputMode.EXPERIENCE);
    }

    public void clearPendingInputs() {
        pendingInputs.clear();
    }

    private void handlePendingInput(Player player, UUID playerId, InputSession session, String input) {
        // A queued value from a previous GUI, quit, or completed session must not affect the player.
        if (!player.isOnline() || pendingInputs.get(playerId) != session) {
            return;
        }
        inputDispatcher.handle(player, session.state(), input, action -> {
            if (pendingInputs.get(playerId) == session && player.isOnline()) {
                action.run();
                pendingInputs.remove(playerId, session);
            }
        });
    }

    private void setInputMode(Player player, ChatInputMode inputState) {
        pendingInputs.put(
                Objects.requireNonNull(player, "player").getUniqueId(),
                new InputSession(Objects.requireNonNull(inputState, "inputState"))
        );
    }

    private void forgetPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        pendingInputs.remove(playerId);
        nicknameManager.forgetPlayer(playerId);
    }

    /** Identity distinguishes two successive sessions even when their modes are identical. */
    private static final class InputSession {
        private final ChatInputMode state;

        InputSession(ChatInputMode state) {
            this.state = state;
        }

        ChatInputMode state() {
            return state;
        }
    }
}
