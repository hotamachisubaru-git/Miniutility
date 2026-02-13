package org.hotamachisubaru.miniutility.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class FoliaUtil {
    private FoliaUtil() {
    }

    public static void runNow(Plugin plugin, Runnable task) {
        if (plugin == null || task == null) {
            return;
        }

        try {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } catch (Throwable ignored) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runLater(Plugin plugin, Runnable task, long delayTicks) {
        if (plugin == null || task == null) {
            return;
        }

        long safeDelay = Math.max(1L, delayTicks);
        try {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), safeDelay);
        } catch (Throwable ignored) {
            Bukkit.getScheduler().runTaskLater(plugin, task, safeDelay);
        }
    }

    public static void runAtPlayer(Plugin plugin, UUID uuid, Runnable task) {
        if (plugin == null || uuid == null || task == null) {
            return;
        }

        Player p = Bukkit.getPlayer(uuid);
        if (p == null) {
            runNow(plugin, task);
            return;
        }

        try {
            p.getScheduler().execute(plugin, task, () -> runNow(plugin, task), 0L);
        } catch (Throwable ignored) {
            runNow(plugin, task);
        }
    }
}
