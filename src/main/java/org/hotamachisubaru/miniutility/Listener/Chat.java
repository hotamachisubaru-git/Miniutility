package org.hotamachisubaru.miniutility.Listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.bukkit.Bukkit.getPluginManager;

public class Chat implements Listener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final Map<UUID, Boolean> waitingForNickname = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> waitingForColorInput = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> waitingForExpInput = new ConcurrentHashMap<>();

    public static void setWaitingForNickname(Player player, boolean waiting) {
        if (waiting) {
            waitingForNickname.put(player.getUniqueId(), true);
        } else {
            waitingForNickname.remove(player.getUniqueId());
        }
    }

    public static boolean isWaitingForNickname(Player player) {
        return waitingForNickname.getOrDefault(player.getUniqueId(), false);
    }

    public static void setWaitingForColorInput(Player player, boolean waiting) {
        if (waiting) {
            waitingForColorInput.put(player.getUniqueId(), true);
        } else {
            waitingForColorInput.remove(player.getUniqueId());
        }
    }

    public static boolean isWaitingForColorInput(Player player) {
        return waitingForColorInput.getOrDefault(player.getUniqueId(), false);
    }

    public static void setWaitingForExpInput(Player player, boolean waiting) {
        if (waiting) {
            waitingForExpInput.put(player.getUniqueId(), true);
        } else {
            waitingForExpInput.remove(player.getUniqueId());
        }
    }

    public static boolean isWaitingForExpInput(Player player) {
        return waitingForExpInput.getOrDefault(player.getUniqueId(), false);
    }

    public static String toPlainText(Component message) {
        if (message == null) return "";
        return PLAIN_TEXT.serialize(message);
    }

    private static Component colored(String message, NamedTextColor color) {
        return Component.text(message, color);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearWaitingState(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        clearWaitingState(event.getPlayer().getUniqueId());
    }

    /**
     * 共通の“待機フラグ”処理。入力を消費したら true
     */
    public static boolean tryHandleWaitingInput(Player player, String plainMessage) {
        if (player == null) return false;

        if (isWaitingForExpInput(player)) {
            setWaitingForExpInput(player, false);
            final String input = plainMessage == null ? "" : plainMessage.trim();
            try {
                final int change = Integer.parseInt(input);
                runOnPlayerThread(player, () -> {
                    int newLevel = Math.max(0, player.getLevel() + change);
                    player.setLevel(newLevel);
                    player.sendMessage(colored("経験値レベルを " + newLevel + " に変更しました。", NamedTextColor.GREEN));
                });
            } catch (NumberFormatException ex) {
                runOnPlayerThread(player, () ->
                        player.sendMessage(colored("数値を入力してください。", NamedTextColor.RED))
                );
            }
            return true;
        }

        if (isWaitingForNickname(player)) {
            final String input = plainMessage == null ? "" : plainMessage.trim();
            String validated = validateNickname(input);
            if (validated != null) {
                setWaitingForNickname(player, false);
                runOnPlayerThread(player, () -> {
                    NicknameManager.setNickname(player, validated);
                    player.sendMessage(colored("ニックネームを「" + validated + "」に設定しました。", NamedTextColor.GREEN));
                });
            } else {
                runOnPlayerThread(player, () ->
                        player.sendMessage(colored("無効なニックネームです。1〜16文字、記号は _- のみ使用可。空白不可。", NamedTextColor.RED))
                );
            }
            return true;
        }

        if (isWaitingForColorInput(player)) {
            setWaitingForColorInput(player, false);

            final String raw = plainMessage == null ? "" : plainMessage.trim();
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

            final String colored = LEGACY_SECTION.serialize(
                    LEGACY_AMPERSAND.deserialize(raw.replace('§', '&'))
            );
            runOnPlayerThread(player, () -> {
                NicknameManager.setNickname(player, colored);
                player.sendMessage(
                        colored("ニックネームを設定しました: ", NamedTextColor.GREEN)
                                .append(LEGACY_SECTION.deserialize(colored))
                );
            });
            return true;
        }

        return false;
    }

    private static void clearWaitingState(UUID uuid) {
        waitingForNickname.remove(uuid);
        waitingForColorInput.remove(uuid);
        waitingForExpInput.remove(uuid);
    }

    private static void runOnPlayerThread(Player player, Runnable task) {
        Plugin plugin = getPluginManager().getPlugin("Miniutility");
        if (plugin == null) {
            task.run();
            return;
        }
        FoliaUtil.runAtPlayer(plugin, player.getUniqueId(), task);
    }

    private static String validateNickname(String s) {
        if (s == null) return null;
        if (s.contains(" ") || s.contains("　")) return null;

        final int len = s.length();
        if (len < 1 || len > 16) return null;

        if (s.matches(".*[<>\"'`$\\\\].*")) return null;

        return s;
    }
}
