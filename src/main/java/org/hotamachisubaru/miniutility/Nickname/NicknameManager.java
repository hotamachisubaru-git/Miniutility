package org.hotamachisubaru.miniutility.Nickname;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.util.LuckPermsUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * ニックネームと表示名を管理するサービス。
 */
public final class NicknameManager {

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final Pattern LEGACY_CODES = Pattern.compile("(?i)[&§][0-9A-FK-OR]");

    private final NicknameDatabase nicknameDatabase;
    private final Logger logger;
    private final Map<UUID, String> nicknameMap = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> prefixEnabled = new ConcurrentHashMap<>();

    public NicknameManager(NicknameDatabase nicknameDatabase, Logger logger) {
        this.nicknameDatabase = Objects.requireNonNull(nicknameDatabase, "nicknameDatabase");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void reload(Collection<? extends Player> onlinePlayers) {
        nicknameMap.clear();
        nicknameMap.putAll(nicknameDatabase.loadAllNicknames());
        for (Player player : onlinePlayers) {
            updateDisplayName(player);
        }
    }

    public void setNickname(Player player, String nickname) {
        if (player == null || nickname == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        nicknameMap.put(uuid, nickname);
        nicknameDatabase.saveNickname(uuid, nickname);
        updateDisplayName(player);
    }

    public void removeNickname(Player player) {
        if (player == null) {
            return;
        }

        nicknameMap.remove(player.getUniqueId());
        nicknameDatabase.deleteNickname(player.getUniqueId());
        updateDisplayName(player);
    }

    public String getDisplayName(Player player) {
        if (player == null) {
            return "";
        }
        String nickname = nicknameMap.get(player.getUniqueId());
        return nickname != null ? nickname : player.getName();
    }

    public boolean togglePrefix(UUID uniqueId) {
        return setPrefixEnabled(uniqueId, !prefixEnabled.getOrDefault(uniqueId, true));
    }

    public boolean setPrefixEnabled(UUID uniqueId, boolean enabled) {
        prefixEnabled.put(uniqueId, enabled);
        Player player = org.bukkit.Bukkit.getPlayer(uniqueId);
        if (player != null) {
            updateDisplayName(player);
        }
        return enabled;
    }

    public boolean setColor(Player player, NamedTextColor color) {
        if (player == null || color == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        String nick = nicknameMap.get(uuid);
        if (nick == null || nick.isEmpty()) {
            return false;
        }

        String base = stripLeadingLegacyCodes(nick);
        String recolored = LEGACY_SECTION.serialize(Component.text(base, color));
        nicknameMap.put(uuid, recolored);
        nicknameDatabase.saveNickname(uuid, recolored);
        updateDisplayName(player);
        return true;
    }

    public void updateDisplayName(Player player) {
        if (player == null) {
            return;
        }

        Component display = buildDisplayComponent(player);
        safelySetDisplayName(player, display);
    }

    public Component buildDisplayComponent(Player player) {
        String nickname = getDisplayName(player);
        boolean showPrefix = prefixEnabled.getOrDefault(player.getUniqueId(), true);
        String prefix = showPrefix ? LuckPermsUtil.safePrefix(player) : "";

        if (!prefix.isEmpty() && nickname.startsWith(prefix)) {
            prefix = "";
        }

        String legacy = prefix.isEmpty() ? nickname : (prefix + "&r " + nickname);
        return toLegacyComponent(legacy);
    }

    public String buildDisplayLegacy(Player player) {
        return LEGACY_SECTION.serialize(buildDisplayComponent(player));
    }

    public Map<UUID, String> snapshotNicknames() {
        return new HashMap<>(nicknameMap);
    }

    private void safelySetDisplayName(Player player, Component displayComponent) {
        try {
            player.displayName(displayComponent);
            player.playerListName(displayComponent);
            return;
        } catch (Throwable ignored) {
            // fallback below
        }

        try {
            String legacy = LEGACY_SECTION.serialize(displayComponent);
            player.setDisplayName(legacy);
            player.setPlayerListName(legacy);
        } catch (Throwable t) {
            logger.warning("表示名更新に失敗しました: " + t.getMessage());
        }
    }

    private static String stripLeadingLegacyCodes(String s) {
        if (s == null) {
            return "";
        }

        int i = 0;
        while (i + 1 < s.length()) {
            char c0 = s.charAt(i);
            char c1 = Character.toLowerCase(s.charAt(i + 1));
            boolean mark = (c0 == '§' || c0 == '&');
            boolean code =
                    (c1 >= '0' && c1 <= '9') ||
                            (c1 >= 'a' && c1 <= 'f') ||
                            (c1 == 'k' || c1 == 'l' || c1 == 'm' || c1 == 'n' || c1 == 'o' || c1 == 'r');
            if (mark && code) {
                i += 2;
            } else {
                break;
            }
        }
        return s.substring(i);
    }

    public static String stripLegacyCodes(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return LEGACY_CODES.matcher(text).replaceAll("");
    }

    private static Component toLegacyComponent(String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_AMPERSAND.deserialize(legacyText.replace('§', '&'));
    }
}
