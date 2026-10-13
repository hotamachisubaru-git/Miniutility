package org.hotamachisubaru.miniutility.registry;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bukkit リスナーの登録を一元管理するクラス。
 * <p>
 * 複数のリスナーを一度に登録するために使用します。
 */
public final class PluginListenerRegistrar {

    private final Plugin plugin;

    public PluginListenerRegistrar(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * 指定されたすべてのリスナーをプラグインに登録します。
     *
     * @param listeners 登録するリスナーの配列
     * @throws NullPointerException リスナー配列またはその要素が null の場合
     */
    public void register(Listener... listeners) {
        Objects.requireNonNull(listeners, "listeners");
        Arrays.stream(listeners).forEach(listener -> Objects.requireNonNull(listener, "listener"));
        var pluginManager = plugin.getServer().getPluginManager();
        for (Listener listener : listeners) {
            pluginManager.registerEvents(listener, plugin);
        }
    }
}
