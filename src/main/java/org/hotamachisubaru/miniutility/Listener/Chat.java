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
import org.hotamachisubaru.miniutility.MiniutilityLoader;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class Chat implements Listener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final Pattern FORBIDDEN_NICKNAME = Pattern.compile("[<>\"'`$\\\\]");

    private final MiniutilityLoader plugin;
    private final NicknameManager nicknameManager;
    private final Map<UUID, WaitingInput> waitingInputByPlayer = new ConcurrentHashMap<>();

    public Chat(MiniutilityLoader plugin, NicknameManager nicknameManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
    }

    public void beginNicknameInput(Player player) {
        waitingInputByPlayer.put(player.getUniqueId(), WaitingInput.NICKNAME);
    }

    public void beginColorNicknameInput(Player player) {
        waitingInputByPlayer.put(player.getUniqueId(), WaitingInput.COLOR_NICKNAME);
    }

    public void beginExpInput(Player player) {
        waitingInputByPlayer.put(player.getUniqueId(), WaitingInput.EXP_CHANGE);
    }

    public static String toPlainText(Component message) {
        if (message == null) {
            return "";
        }
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

    public boolean tryHandleWaitingInput(Player player, String plainMessage) {
        if (player == null) {
            return false;
        }

        WaitingInput waitingInput = waitingInputByPlayer.remove(player.getUniqueId());
        if (waitingInput == null) {
            return false;
        }

        String input = plainMessage == null ? "" : plainMessage.trim();
        return switch (waitingInput) {
            case EXP_CHANGE -> handleExpInput(player, input);
            case NICKNAME -> handleNicknameInput(player, input);
            case COLOR_NICKNAME -> handleColorNicknameInput(player, input);
        };
    }

    private boolean handleExpInput(Player player, String input) {
        try {
            int change = Integer.parseInt(input);
            runOnPlayerThread(player, () -> {
                int newLevel = Math.max(0, player.getLevel() + change);
                player.setLevel(newLevel);
                player.sendMessage(colored("経験値レベルを " + newLevel + " に変更しました。", NamedTextColor.GREEN));
            });
        } catch (NumberFormatException ex) {
            runOnPlayerThread(player, () -> player.sendMessage(colored("数値を入力してください。", NamedTextColor.RED)));
        }
        return true;
    }

    private boolean handleNicknameInput(Player player, String input) {
        String validated = validateNickname(input);
        if (validated == null) {
            runOnPlayerThread(player, () ->
                    player.sendMessage(colored("無効なニックネームです。1〜16文字、空白不可、危険文字不可。", NamedTextColor.RED))
            );
            return true;
        }

        runOnPlayerThread(player, () -> {
            nicknameManager.setNickname(player, validated);
            player.sendMessage(colored("ニックネームを「" + validated + "」に設定しました。", NamedTextColor.GREEN));
        });
        return true;
    }

    private boolean handleColorNicknameInput(Player player, String rawInput) {
        if (rawInput.isBlank()) {
            runOnPlayerThread(player, () -> player.sendMessage(colored("例: &6ほたまち", NamedTextColor.RED)));
            return true;
        }

        String normalized = rawInput.replace('§', '&');
        String visibleText = NicknameManager.stripLegacyCodes(normalized);
        String validatedVisible = validateNickname(visibleText);
        if (validatedVisible == null) {
            runOnPlayerThread(player, () ->
                    player.sendMessage(colored("無効なニックネームです。1〜16文字、空白不可、危険文字不可。", NamedTextColor.RED))
            );
            return true;
        }

        String coloredNickname = LEGACY_SECTION.serialize(
                LEGACY_AMPERSAND.deserialize(normalized)
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

    private void runOnPlayerThread(Player player, Runnable task) {
        FoliaUtil.runAtPlayer(plugin, player.getUniqueId(), task);
    }

    private static String validateNickname(String value) {
        if (value == null) {
            return null;
        }
        if (value.contains(" ") || value.contains("　")) {
            return null;
        }

        int length = value.length();
        if (length < 1 || length > 16) {
            return null;
        }

        if (FORBIDDEN_NICKNAME.matcher(value).find()) {
            return null;
        }

        return value;
    }

    private void clearWaitingState(UUID uuid) {
        waitingInputByPlayer.remove(uuid);
    }

    private enum WaitingInput {
        NICKNAME,
        COLOR_NICKNAME,
        EXP_CHANGE
    }
}
