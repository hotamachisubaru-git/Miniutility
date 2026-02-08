package org.hotamachisubaru.miniutility.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class FoliaUtil {
    private FoliaUtil() {
    }

    public static void runNow(Plugin plugin, Runnable task) {
        if (plugin == null || task == null) return;
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    public static void runLater(Plugin plugin, Runnable task, long delayTicks) {
        if (plugin == null || task == null) return;
        long safeDelay = Math.max(1L, delayTicks);
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), safeDelay);
    }

    public static void runAtPlayer(Plugin plugin, UUID uuid, Runnable task) {
        if (plugin == null || uuid == null || task == null) return;
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;
        p.getScheduler().execute(plugin, task, null, 1L);
    }
}
