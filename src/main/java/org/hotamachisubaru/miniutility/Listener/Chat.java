package org.hotamachisubaru.miniutility.Listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.hotamachisubaru.miniutility.Nickname.NicknameValidator;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.util.ComponentUtil;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Chat implements Listener {

    private final Plugin plugin;
    private final NicknameManager nicknameManager;
    private final Map<UUID, Boolean> waitingForNickname = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> waitingForColorInput = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> waitingForExpInput = new ConcurrentHashMap<>();

    public Chat(Plugin plugin, NicknameManager nicknameManager) {
        this.plugin = plugin;
        this.nicknameManager = nicknameManager;
    }

    public void awaitNicknameInput(Player player) {
        setWaitingState(waitingForNickname, player, true);
    }

    public void awaitColorNicknameInput(Player player) {
        setWaitingState(waitingForColorInput, player, true);
    }

    public void awaitExpInput(Player player) {
        setWaitingState(waitingForExpInput, player, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChatEvent(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plain = ComponentUtil.plain(event.message());
        if (tryHandleWaitingInput(player, plain)) {
            event.setCancelled(true);
            return;
        }

        Component display = nicknameManager.getDisplayNameComponent(player);
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
                ComponentUtil.chatMessage(display, message)
        ));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        nicknameManager.updateDisplayName(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearWaitingState(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        clearWaitingState(event.getPlayer().getUniqueId());
    }

    private boolean tryHandleWaitingInput(Player player, String plainMessage) {
        if (player == null) {
            return false;
        }

        if (isWaiting(waitingForExpInput, player)) {
            String input = plainMessage == null ? "" : plainMessage.trim();
            try {
                int change = Integer.parseInt(input);
                setWaitingState(waitingForExpInput, player, false);
                runOnPlayerThread(player, () -> {
                    int beforeLevel = player.getLevel();
                    int appliedChange = Math.max(-beforeLevel, change);
                    player.giveExpLevels(appliedChange);

                    int afterLevel = player.getLevel();
                    String deltaText = (appliedChange > 0 ? "+" : "") + appliedChange;
                    player.sendMessage(ComponentUtil.text(
                            "経験値レベルを " + deltaText + " しました。現在レベル: " + afterLevel,
                            NamedTextColor.GREEN
                    ));
                });
            } catch (NumberFormatException exception) {
                runOnPlayerThread(player, () ->
                        player.sendMessage(ComponentUtil.text("数値を入力してください。例: 99 または -5", NamedTextColor.RED))
                );
            }
            return true;
        }

        if (isWaiting(waitingForNickname, player)) {
            String input = plainMessage == null ? "" : plainMessage.trim();
            String validated = NicknameValidator.validatePlain(input);
            if (validated != null) {
                setWaitingState(waitingForNickname, player, false);
                runOnPlayerThread(player, () -> {
                    nicknameManager.setNickname(player, validated);
                    player.sendMessage(ComponentUtil.text("ニックネームを「" + validated + "」に設定しました。", NamedTextColor.GREEN));
                });
            } else {
                runOnPlayerThread(player, () ->
                        player.sendMessage(ComponentUtil.text("無効なニックネームです。1〜16文字、記号は _- のみ使用可。空白不可。", NamedTextColor.RED))
                );
            }
            return true;
        }

        if (isWaiting(waitingForColorInput, player)) {
            String raw = plainMessage == null ? "" : plainMessage.trim();
            if (raw.isEmpty()) {
                runOnPlayerThread(player, () ->
                        player.sendMessage(ComponentUtil.text("例: &6a, &bほたまち", NamedTextColor.RED))
                );
                return true;
            }

            String visible = NicknameValidator.visibleWithoutLegacyCodes(raw);
            if (NicknameValidator.validatePlain(visible) == null) {
                runOnPlayerThread(player, () ->
                        player.sendMessage(ComponentUtil.text("無効なニックネームです。1〜16文字、記号は _- のみ、空白不可。", NamedTextColor.RED))
                );
                return true;
            }

            setWaitingState(waitingForColorInput, player, false);
            String coloredNickname = ComponentUtil.ampersandToSection(raw);
            runOnPlayerThread(player, () -> {
                nicknameManager.setNickname(player, coloredNickname);
                player.sendMessage(
                        ComponentUtil.append(
                                ComponentUtil.text("ニックネームを設定しました: ", NamedTextColor.GREEN),
                                ComponentUtil.legacySection(coloredNickname)
                        )
                );
            });
            return true;
        }

        return false;
    }

    private void runOnPlayerThread(Player player, Runnable task) {
        if (!plugin.isEnabled()) {
            task.run();
            return;
        }
        FoliaUtil.runAtPlayer(plugin, player.getUniqueId(), task);
    }

    private void clearWaitingState(UUID uniqueId) {
        waitingForNickname.remove(uniqueId);
        waitingForColorInput.remove(uniqueId);
        waitingForExpInput.remove(uniqueId);
    }

    private static boolean isWaiting(Map<UUID, Boolean> state, Player player) {
        return state.getOrDefault(player.getUniqueId(), false);
    }

    private static void setWaitingState(Map<UUID, Boolean> state, Player player, boolean waiting) {
        if (waiting) {
            state.put(player.getUniqueId(), true);
        } else {
            state.remove(player.getUniqueId());
        }
    }
}
