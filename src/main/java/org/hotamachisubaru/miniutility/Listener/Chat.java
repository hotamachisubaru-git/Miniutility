package org.hotamachisubaru.miniutility.Listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.hotamachisubaru.miniutility.MiniutilityLoader;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Chat implements Listener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private final MiniutilityLoader plugin;
    private final NicknameManager nicknameManager;
    private final Map<UUID, Boolean> waitingForNickname = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> waitingForColorInput = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> waitingForExpInput = new ConcurrentHashMap<>();

    public Chat(MiniutilityLoader plugin, NicknameManager nicknameManager) {
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
        String plain = toPlainText(event.message());
        if (tryHandleWaitingInput(player, plain)) {
            event.setCancelled(true);
            return;
        }

        Component display = nicknameManager.getDisplayNameComponent(player);
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) -> {
            Component renderedMessage = Component.empty()
                    .append(display)
                    .append(Component.text(" » "))
                    .append(message);
            return Objects.requireNonNull(renderedMessage);
        }));
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
                    player.sendMessage(colored(
                            "経験値レベルを " + deltaText + " しました。現在レベル: " + afterLevel,
                            NamedTextColor.GREEN
                    ));
                });
            } catch (NumberFormatException exception) {
                runOnPlayerThread(player, () ->
                        player.sendMessage(colored("数値を入力してください。例: 99 または -5", NamedTextColor.RED))
                );
            }
            return true;
        }

        if (isWaiting(waitingForNickname, player)) {
            String input = plainMessage == null ? "" : plainMessage.trim();
            String validated = validateNickname(input);
            if (validated != null) {
                setWaitingState(waitingForNickname, player, false);
                runOnPlayerThread(player, () -> {
                    nicknameManager.setNickname(player, validated);
                    player.sendMessage(colored("ニックネームを「" + validated + "」に設定しました。", NamedTextColor.GREEN));
                });
            } else {
                runOnPlayerThread(player, () ->
                        player.sendMessage(colored("無効なニックネームです。1〜16文字、記号は _- のみ使用可。空白不可。", NamedTextColor.RED))
                );
            }
            return true;
        }

        if (isWaiting(waitingForColorInput, player)) {
            setWaitingState(waitingForColorInput, player, false);

            String raw = plainMessage == null ? "" : plainMessage.trim();
            if (raw.isEmpty()) {
                runOnPlayerThread(player, () ->
                        player.sendMessage(colored("例: &6a, &bほたまち", NamedTextColor.RED))
                );
                return true;
            }

            String visible = raw.replaceAll("(?i)[&§][0-9a-fk-or]", "");
            if (validateNickname(visible) == null) {
                runOnPlayerThread(player, () ->
                        player.sendMessage(colored("無効なニックネームです。1〜16文字、記号は _- のみ、空白不可。", NamedTextColor.RED))
                );
                return true;
            }

            String coloredNickname = LEGACY_SECTION.serialize(
                    LEGACY_AMPERSAND.deserialize(raw.replace('§', '&'))
            );
            runOnPlayerThread(player, () -> {
                nicknameManager.setNickname(player, coloredNickname);
                player.sendMessage(
                        colored("ニックネームを設定しました: ", NamedTextColor.GREEN)
                                .append(LEGACY_SECTION.deserialize(coloredNickname))
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

    private static Component colored(String message, NamedTextColor color) {
        return Component.text(message, color);
    }

    private static String toPlainText(Component message) {
        if (message == null) {
            return "";
        }
        return PLAIN_TEXT.serialize(message);
    }

    private static String validateNickname(String input) {
        if (input == null) {
            return null;
        }
        if (input.contains(" ") || input.contains("　")) {
            return null;
        }
        if (input.contains("&") || input.contains("§")) {
            return null;
        }
        if (input.length() < 1 || input.length() > 16) {
            return null;
        }
        if (input.matches(".*[<>\"'`$\\\\].*")) {
            return null;
        }
        return input;
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
