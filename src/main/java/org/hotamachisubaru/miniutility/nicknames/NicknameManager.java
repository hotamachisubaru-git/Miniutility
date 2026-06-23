package org.hotamachisubaru.miniutility.nicknames;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.Miniutility;
import org.hotamachisubaru.miniutility.util.ComponentUtil;
import org.hotamachisubaru.miniutility.util.LuckPermsUtil;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NicknameManager {

    private final Miniutility plugin;
    private final Logger logger;
    private final NicknameDatabase nicknameDatabase;
    private final ConcurrentMap<UUID, String> nicknames = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> prefixOverrides = new ConcurrentHashMap<>();

    public NicknameManager(Miniutility plugin, NicknameDatabase nicknameDatabase) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.nicknameDatabase = Objects.requireNonNull(nicknameDatabase, "nicknameDatabase");
    }

    public void reload() {
        Map<UUID, String> loadedNicknames = nicknameDatabase.loadAll();
        nicknames.clear();
        nicknames.putAll(loadedNicknames);
        refreshOnlinePlayers();
    }

    public void persistAll() {
        nicknameDatabase.saveAll(Map.copyOf(nicknames));
    }

    public void refreshOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateDisplayName(player);
        }
    }

    public void setNickname(Player player, String nickname) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(nickname, "nickname");
        UUID playerId = player.getUniqueId();
        nicknameDatabase.saveNickname(playerId, nickname);
        nicknames.put(playerId, nickname);
        updateDisplayName(player);
    }

    public void removeNickname(Player player) {
        Objects.requireNonNull(player, "player");
        UUID playerId = player.getUniqueId();
        nicknameDatabase.deleteNickname(playerId);
        nicknames.remove(playerId);
        updateDisplayName(player);
    }

    public String getDisplayName(Player player) {
        Objects.requireNonNull(player, "player");
        return nicknames.getOrDefault(player.getUniqueId(), player.getName());
    }

    public Component getDisplayNameComponent(Player player) {
        Objects.requireNonNull(player, "player");
        String nickname = getDisplayName(player);
        String prefix = isPrefixEnabled(player.getUniqueId()) ? LuckPermsUtil.prefixOrEmpty(player) : "";
        if (!prefix.isEmpty() && nickname.startsWith(prefix)) {
            prefix = "";
        }

        String displayText = prefix.isEmpty() ? nickname : prefix + "&r " + nickname;
        return ComponentUtil.legacy(displayText);
    }

    public void updateDisplayName(Player player) {
        Objects.requireNonNull(player, "player");
        Component displayComponent = getDisplayNameComponent(player);
        try {
            player.displayName(displayComponent);
            player.playerListName(displayComponent);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "表示名の更新に失敗しました。", exception);
        }
    }

    public boolean togglePrefix(UUID uniqueId) {
        boolean nextState = !isPrefixEnabled(uniqueId);
        setPrefixEnabled(uniqueId, nextState);
        return nextState;
    }

    public void setPrefixEnabled(UUID uniqueId, boolean enabled) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        prefixOverrides.put(uniqueId, enabled);
        Player player = Bukkit.getPlayer(uniqueId);
        if (player != null) {
            updateDisplayName(player);
        }
    }

    private boolean isPrefixEnabled(UUID uniqueId) {
        return prefixOverrides.getOrDefault(
                uniqueId,
                plugin.getConfig().getBoolean("combine-prefix", true)
        );
    }
}
