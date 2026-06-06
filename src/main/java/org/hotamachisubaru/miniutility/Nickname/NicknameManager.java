package org.hotamachisubaru.miniutility.Nickname;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.Miniutility;
import org.hotamachisubaru.miniutility.util.ComponentUtil;
import org.hotamachisubaru.miniutility.util.LuckPermsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class NicknameManager {

    private final Miniutility plugin;
    private final Logger logger;
    private final NicknameDatabase nicknameDatabase;
    private final Map<UUID, String> nicknameMap = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> prefixEnabled = new ConcurrentHashMap<>();

    public NicknameManager(Miniutility plugin, NicknameDatabase nicknameDatabase) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.nicknameDatabase = nicknameDatabase;
    }

    public void reload() {
        nicknameMap.clear();
        nicknameMap.putAll(nicknameDatabase.loadAll());
        refreshOnlinePlayers();
    }

    public void persistAll() {
        nicknameDatabase.saveAll(nicknameMap);
    }

    public void refreshOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateDisplayName(player);
        }
    }

    public void setNickname(Player player, String nickname) {
        if (player == null || nickname == null) {
            return;
        }

        nicknameMap.put(player.getUniqueId(), nickname);
        nicknameDatabase.saveNickname(player.getUniqueId(), nickname);
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
        return nicknameMap.getOrDefault(player.getUniqueId(), player.getName());
    }

    public @NotNull Component getDisplayNameComponent(@Nullable Player player) {
        if (player == null) {
            return ComponentUtil.empty();
        }

        String nickname = getDisplayName(player);
        String prefix = isPrefixEnabled(player.getUniqueId()) ? LuckPermsUtil.safePrefix(player) : "";
        if (!prefix.isEmpty() && nickname.startsWith(prefix)) {
            prefix = "";
        }

        String displayText = prefix.isEmpty() ? nickname : prefix + "&r " + nickname;
        return toLegacyComponent(displayText);
    }

    public void updateDisplayName(Player player) {
        if (player == null) {
            return;
        }

        Component displayComponent = getDisplayNameComponent(player);
        try {
            player.displayName(displayComponent);
            player.playerListName(displayComponent);
        } catch (Throwable throwable) {
            logger.warning("表示名更新に失敗しました: " + throwable.getMessage());
        }
    }

    public boolean togglePrefix(UUID uniqueId) {
        boolean nextState = !isPrefixEnabled(uniqueId);
        setPrefixEnabled(uniqueId, nextState);
        return nextState;
    }

    public void setPrefixEnabled(UUID uniqueId, boolean enabled) {
        prefixEnabled.put(uniqueId, enabled);
        Player player = Bukkit.getPlayer(uniqueId);
        if (player != null) {
            updateDisplayName(player);
        }
    }

    public boolean setColor(Player player, NamedTextColor color) {
        if (player == null || color == null) {
            return false;
        }

        UUID uniqueId = player.getUniqueId();
        String nickname = nicknameMap.get(uniqueId);
        if (nickname == null || nickname.isEmpty()) {
            return false;
        }

        String base = NicknameValidator.stripLeadingLegacyCodes(nickname);
        String recolored = ComponentUtil.serializeSection(ComponentUtil.text(base, color));
        nicknameMap.put(uniqueId, recolored);
        nicknameDatabase.saveNickname(uniqueId, recolored);
        updateDisplayName(player);
        return true;
    }

    private boolean isPrefixEnabled(UUID uniqueId) {
        return prefixEnabled.getOrDefault(uniqueId, plugin.getConfig().getBoolean("combine-prefix", true));
    }

    private static @NotNull Component toLegacyComponent(@Nullable String legacyText) {
        return ComponentUtil.legacy(legacyText);
    }
}
