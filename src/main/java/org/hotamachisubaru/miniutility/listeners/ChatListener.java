package org.hotamachisubaru.miniutility.listeners;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.hotamachisubaru.miniutility.nicknames.NicknameManager;
import org.hotamachisubaru.miniutility.nicknames.NicknameValidator;
import org.hotamachisubaru.miniutility.util.ComponentUtil;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ChatListener implements Listener {

    private final Plugin plugin;
    private final NicknameManager nicknameManager;
    private final ConcurrentMap<UUID, InputMode> pendingInputs = new ConcurrentHashMap<>();

    public ChatListener(Plugin plugin, NicknameManager nicknameManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
    }

    public void awaitNicknameInput(Player player) {
        setInputMode(player, InputMode.NICKNAME);
    }

    public void awaitColorNicknameInput(Player player) {
        setInputMode(player, InputMode.COLORED_NICKNAME);
    }

    public void awaitExpInput(Player player) {
        setInputMode(player, InputMode.EXPERIENCE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = ComponentUtil.plain(event.message());
        if (handlePendingInput(player, message)) {
            event.setCancelled(true);
            return;
        }

        Component displayName = nicknameManager.getDisplayNameComponent(player);
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, chatMessage) ->
                ComponentUtil.chatMessage(displayName, chatMessage)
        ));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        nicknameManager.updateDisplayName(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    private boolean handlePendingInput(Player player, String message) {
        UUID playerId = player.getUniqueId();
        InputMode inputMode = pendingInputs.get(playerId);
        if (inputMode == null) {
            return false;
        }

        String input = message.trim();
        return switch (inputMode) {
            case EXPERIENCE -> handleExperienceInput(player, input);
            case NICKNAME -> handleNicknameInput(player, input);
            case COLORED_NICKNAME -> handleColoredNicknameInput(player, input);
        };
    }

    private boolean handleExperienceInput(Player player, String input) {
        final int requestedChange;
        try {
            requestedChange = Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            sendMessage(player, "数値を入力してください。例: 99 または -5", NamedTextColor.RED);
            return true;
        }

        completeInput(player, InputMode.EXPERIENCE, () -> {
            int currentLevel = player.getLevel();
            int appliedChange = Math.clamp(requestedChange, -currentLevel, Integer.MAX_VALUE - currentLevel);
            player.giveExpLevels(appliedChange);

            String signedChange = appliedChange > 0 ? "+" + appliedChange : Integer.toString(appliedChange);
            player.sendMessage(ComponentUtil.text(
                    "経験値レベルを " + signedChange + " しました。現在レベル: " + player.getLevel(),
                    NamedTextColor.GREEN
            ));
        });
        return true;
    }

    private boolean handleNicknameInput(Player player, String input) {
        if (!NicknameValidator.isValidPlainNickname(input)) {
            sendMessage(player, "無効なニックネームです。1〜16文字、記号は _- のみ使用可。空白不可。", NamedTextColor.RED);
            return true;
        }

        String nickname = input;
        completeInput(player, InputMode.NICKNAME, () -> {
            nicknameManager.setNickname(player, nickname);
            player.sendMessage(ComponentUtil.text(
                    "ニックネームを「" + nickname + "」に設定しました。",
                    NamedTextColor.GREEN
            ));
        });
        return true;
    }

    private boolean handleColoredNicknameInput(Player player, String input) {
        String visibleNickname = NicknameValidator.visibleWithoutLegacyCodes(input);
        if (!NicknameValidator.isValidPlainNickname(visibleNickname)) {
            sendMessage(player, "無効なニックネームです。1〜16文字、記号は _- のみ、空白不可。", NamedTextColor.RED);
            return true;
        }

        String coloredNickname = ComponentUtil.ampersandToSection(input);
        completeInput(player, InputMode.COLORED_NICKNAME, () -> {
            nicknameManager.setNickname(player, coloredNickname);
            player.sendMessage(
                    ComponentUtil.text("ニックネームを設定しました: ", NamedTextColor.GREEN)
                            .append(ComponentUtil.legacySection(coloredNickname))
            );
        });
        return true;
    }

    private void completeInput(Player player, InputMode expectedMode, Runnable action) {
        UUID playerId = player.getUniqueId();
        if (pendingInputs.remove(playerId, expectedMode)) {
            runOnServerThread(action);
        }
    }

    private void sendMessage(Player player, String message, NamedTextColor color) {
        runOnServerThread(() -> player.sendMessage(ComponentUtil.text(message, color)));
    }

    private void runOnServerThread(Runnable task) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private void setInputMode(Player player, InputMode inputMode) {
        pendingInputs.put(
                Objects.requireNonNull(player, "player").getUniqueId(),
                Objects.requireNonNull(inputMode, "inputMode")
        );
    }

    private enum InputMode {
        NICKNAME,
        COLORED_NICKNAME,
        EXPERIENCE
    }
}
