package org.hotamachisubaru.miniutility.registry;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class ListenerRegistry {

    private final Plugin plugin;

    public ListenerRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(Listener... listeners) {
        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
}
