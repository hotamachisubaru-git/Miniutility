package org.hotamachisubaru.miniutility.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Keeps the optional LuckPerms API behind a lazily loaded bridge. */
public final class LuckPermsPrefixProvider {

    private static final Supplier<String> NO_PREFIX = () -> "";

    private LuckPermsPrefixProvider() {
    }

    /** Call on the server thread: PlayerAdapter may evaluate Bukkit-backed contexts. */
    public static String prefixOrEmpty(Player player) {
        return capturePrefixLookup(player).get();
    }

    /**
     * Captures the player's query options on the server thread. The returned lookup only
     * reads LuckPerms' cached metadata, so chat threads do not invoke context calculators.
     * Reading the cache each time also preserves live prefix/permission updates.
     */
    public static Supplier<String> capturePrefixLookup(Player player) {
        Objects.requireNonNull(player, "player");
        try {
            return Bridge.capture(player);
        } catch (IllegalStateException | NoClassDefFoundError unavailable) {
            return NO_PREFIX;
        }
    }

    /** Plugin-owned subscriptions are automatically released by LuckPerms on disable. */
    public static void registerRefreshListeners(JavaPlugin plugin, Consumer<UUID> refreshPlayer) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(refreshPlayer, "refreshPlayer");
        try {
            Bridge.register(plugin, refreshPlayer);
        } catch (IllegalStateException | NoClassDefFoundError unavailable) {
            // LuckPerms is optional, including during plugin startup and shutdown.
        }
    }

    private static final class Bridge {
        private static Supplier<String> capture(Player player) {
            var adapter = net.luckperms.api.LuckPermsProvider.get().getPlayerAdapter(Player.class);
            var cachedData = adapter.getUser(player).getCachedData();
            var queryOptions = adapter.getQueryOptions(player);
            return () -> {
                try {
                    return Objects.requireNonNullElse(cachedData.getMetaData(queryOptions).getPrefix(), "");
                } catch (IllegalStateException unavailable) {
                    return "";
                }
            };
        }

        private static void register(JavaPlugin plugin, Consumer<UUID> refreshPlayer) {
            var eventBus = net.luckperms.api.LuckPermsProvider.get().getEventBus();
            eventBus.subscribe(plugin, net.luckperms.api.event.context.ContextUpdateEvent.class,
                    event -> event.getSubject(Player.class)
                            .ifPresent(player -> refreshPlayer.accept(player.getUniqueId())));
            eventBus.subscribe(plugin, net.luckperms.api.event.user.UserDataRecalculateEvent.class,
                    event -> refreshPlayer.accept(event.getUser().getUniqueId()));
        }
    }
}