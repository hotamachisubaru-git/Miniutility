package org.hotamachisubaru.miniutility.registry;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class ListenerRegistry {

    private final Plugin plugin;

    public ListenerRegistry(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void register(Listener... listeners) {
        Objects.requireNonNull(listeners, "listeners");
        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(
                    Objects.requireNonNull(listener, "listener"),
                    plugin
            );
        }
    }
}
