package org.hotamachisubaru.miniutility.Nickname;

import net.luckperms.api.cacheddata.CachedMetaData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * ニックネーム管理（Paper 1.21.11 ネイティブ）
 */
public class NicknameManager {

    private static final Logger logger = Bukkit.getLogger();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    /** uuid -> nickname (&/§ レガシーコード可) */
    public static final Map<UUID, String> nicknameMap = new ConcurrentHashMap<>();

    /** uuid -> prefix 表示のON/OFF */
    private static final Map<UUID, Boolean> prefixEnabled = new ConcurrentHashMap<>();

    /** DB アクセス */
    private static NicknameDatabase nicknameDatabase = new NicknameDatabase();

    public NicknameManager(NicknameDatabase nicknameDatabase) {
        NicknameManager.nicknameDatabase = nicknameDatabase;
    }

    public static void init() {
        NicknameDatabase.init();
        NicknameDatabase.reload();
    }

    public static void setNickname(Player player, String nickname) {
        if (player == null || nickname == null) return;
        nicknameDatabase.setNickname(player.getUniqueId().toString(), nickname);
        nicknameMap.put(player.getUniqueId(), nickname);
        updateDisplayName(player);
    }

    public static void removeNickname(Player player) {
        if (player == null) return;
        nicknameMap.remove(player.getUniqueId());
        NicknameDatabase.deleteNickname(player);
        updateDisplayName(player);
    }

    public static String getDisplayName(Player player) {
        if (player == null) return "";
        String nickname = nicknameMap.get(player.getUniqueId());
        return (nickname != null) ? nickname : player.getName();
    }

    /**
     * Prefix の表示をトグル。戻り値は新しい状態（true=表示）
     */
    public boolean togglePrefix(UUID uniqueId) {
        boolean newState = !prefixEnabled.getOrDefault(uniqueId, true);
        prefixEnabled.put(uniqueId, newState);

        Player player = Bukkit.getPlayer(uniqueId);
        if (player != null) {
            updateDisplayName(player);
        }
        return newState;
    }

    /**
     * 先頭の色/装飾コードを外して、新しい色で付け直し
     */
    public static boolean setColor(Player player, NamedTextColor color) {
        if (player == null || color == null) return false;
        UUID uuid = player.getUniqueId();
        String nick = nicknameMap.get(uuid);
        if (nick == null || nick.isEmpty()) return false;

        String base = stripLeadingLegacyCodes(nick);
        String recolored = LEGACY_SECTION.serialize(Component.text(base, color));
        nicknameMap.put(uuid, recolored);
        nicknameDatabase.setNickname(uuid.toString(), recolored);
        updateDisplayName(player);
        return true;
    }

    /**
     * 表示名（チャット名／タブ名）を更新
     */
    public static void updateDisplayName(Player player) {
        if (player == null) return;

        String nickname = getDisplayName(player);
        String prefix = "";
        try {
            boolean show = prefixEnabled.getOrDefault(player.getUniqueId(), true);
            if (show) {
                CachedMetaData meta = net.luckperms.api.LuckPermsProvider.get()
                        .getPlayerAdapter(Player.class).getMetaData(player);
                prefix = (meta.getPrefix() == null) ? "" : meta.getPrefix();
            }
        } catch (Throwable ignored) {
        }

        Component displayComponent = toLegacyComponent(prefix + nickname);
        try {
            player.displayName(displayComponent);
            player.playerListName(displayComponent);
        } catch (Throwable t) {
            logger.warning("表示名更新に失敗しました: " + t.getMessage());
        }
    }

    private static String stripLeadingLegacyCodes(String s) {
        if (s == null) return null;
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

    private static Component toLegacyComponent(String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) return Component.empty();
        return LEGACY_AMPERSAND.deserialize(legacyText.replace('§', '&'));
    }
}
