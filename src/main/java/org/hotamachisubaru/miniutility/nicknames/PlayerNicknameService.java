package org.hotamachisubaru.miniutility.nicknames;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.hotamachisubaru.miniutility.MiniutilityPlugin;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.hotamachisubaru.miniutility.util.LuckPermsPrefixProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persists nicknames and publishes display-name snapshots for Paper's chat threads. */
public final class PlayerNicknameService {

    private static final String CONFIG_PREFIX_KEY = "combine-prefix";

    private final MiniutilityPlugin plugin;
    private final Logger logger;
    private final SqliteNicknameRepository nicknameDatabase;
    private volatile Map<UUID, String> nicknames = Map.of();
    private final ConcurrentMap<UUID, Boolean> prefixOverrides = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, DisplaySnapshot> displaySnapshots = new ConcurrentHashMap<>();

    public PlayerNicknameService(MiniutilityPlugin plugin, SqliteNicknameRepository nicknameDatabase) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.nicknameDatabase = Objects.requireNonNull(nicknameDatabase, "nicknameDatabase");
        LuckPermsPrefixProvider.registerRefreshListeners(plugin, uniqueId -> {
            // Never re-enter a LuckPerms context update while its event is still dispatching.
            scheduleWhileEnabled(() -> refreshPlayer(uniqueId));
        });
    }

    /** Replace the complete immutable nickname map before refreshing player-facing state. */
    public synchronized void reload() {
        nicknames = Map.copyOf(nicknameDatabase.loadAll());
        refreshOnlinePlayers();
    }

    public void persistAll() {
        nicknameDatabase.saveAll(nicknames);
    }

    public void refreshOnlinePlayers() {
        displaySnapshots.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateDisplayName(player);
        }
    }

    /** Keep the in-memory name unchanged when a database write fails. */
    public synchronized void setNickname(Player player, String nickname) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(nickname, "nickname");
        UUID playerId = player.getUniqueId();
        nicknameDatabase.saveNickname(playerId, nickname);
        var updated = new HashMap<>(nicknames);
        updated.put(playerId, nickname);
        nicknames = Map.copyOf(updated);
        updateDisplayName(player);
    }

    public synchronized void removeNickname(Player player) {
        Objects.requireNonNull(player, "player");
        UUID playerId = player.getUniqueId();
        nicknameDatabase.deleteNickname(playerId);
        var updated = new HashMap<>(nicknames);
        updated.remove(playerId);
        nicknames = Map.copyOf(updated);
        updateDisplayName(player);
    }

    public String getDisplayName(Player player) {
        Objects.requireNonNull(player, "player");
        return nicknames.getOrDefault(player.getUniqueId(), player.getName());
    }

    /** Call on the server thread to capture the current player name and LuckPerms contexts. */
    public Component getDisplayNameComponent(Player player) {
        return snapshot(Objects.requireNonNull(player, "player")).component();
    }

    /** No Bukkit/configuration/PlayerAdapter calls occur during this asynchronous read. */
    public Optional<Component> getCachedDisplayNameComponent(UUID playerId) {
        DisplaySnapshot snapshot = displaySnapshots.get(Objects.requireNonNull(playerId, "playerId"));
        return snapshot == null ? Optional.empty() : Optional.of(snapshot.component());
    }

    public void updateDisplayName(Player player) {
        Objects.requireNonNull(player, "player");
        DisplaySnapshot snapshot = snapshot(player);
        displaySnapshots.put(player.getUniqueId(), snapshot);
        Component displayName = snapshot.component();
        try {
            player.displayName(displayName);
            player.playerListName(displayName);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "表示名の更新に失敗しました。", exception);
        }
    }

    /** Display snapshots hold no Player references and are removed when the player leaves. */
    public void forgetPlayer(UUID playerId) {
        displaySnapshots.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public boolean togglePrefix(UUID uniqueId) {
        boolean enabled = !isPrefixEnabled(Objects.requireNonNull(uniqueId, "uniqueId"));
        setPrefixEnabled(uniqueId, enabled);
        return enabled;
    }

    public void setPrefixEnabled(UUID uniqueId, boolean enabled) {
        prefixOverrides.put(Objects.requireNonNull(uniqueId, "uniqueId"), enabled);
        refreshPlayer(uniqueId);
    }

    /** Uses this plugin as task owner; asynchronous chat actions must never use a null owner. */
    public void runOnServerThread(Runnable action) {
        Objects.requireNonNull(action, "action");
        if (!plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            action.run();
        } else {
            scheduleWhileEnabled(action);
        }
    }

    private void scheduleWhileEnabled(Runnable action) {
        if (!plugin.isEnabled()) {
            return;
        }
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (plugin.isEnabled()) {
                    action.run();
                }
            });
        } catch (IllegalPluginAccessException disabledDuringScheduling) {
            if (plugin.isEnabled()) {
                throw disabledDuringScheduling;
            }
        }
    }

    private void refreshPlayer(UUID uniqueId) {
        runOnServerThread(() -> {
            Player player = Bukkit.getPlayer(uniqueId);
            if (player != null && player.isOnline()) {
                updateDisplayName(player);
            }
        });
    }

    private DisplaySnapshot snapshot(Player player) {
        return new DisplaySnapshot(
                getDisplayName(player),
                isPrefixEnabled(player.getUniqueId()) ? LuckPermsPrefixProvider.capturePrefixLookup(player) : () -> ""
        );
    }

    private boolean isPrefixEnabled(UUID uniqueId) {
        return prefixOverrides.getOrDefault(uniqueId, plugin.getConfig().getBoolean(CONFIG_PREFIX_KEY, true));
    }

    private record DisplaySnapshot(String nickname, Supplier<String> prefixLookup) {
        Component component() {
            String prefix = prefixLookup.get();
            if (prefix.isEmpty() || nickname.startsWith(prefix)) {
                return LegacyTextFormatter.legacy(nickname);
            }
            return LegacyTextFormatter.legacy(prefix + "&r " + nickname);
        }
    }
}
