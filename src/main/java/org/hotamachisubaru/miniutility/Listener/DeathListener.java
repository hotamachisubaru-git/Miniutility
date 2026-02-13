package org.hotamachisubaru.miniutility.Listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.hotamachisubaru.miniutility.Miniutility;

import java.util.Objects;

public final class DeathListener implements Listener {

    private final Miniutility miniutility;

    public DeathListener(Miniutility miniutility) {
        this.miniutility = Objects.requireNonNull(miniutility, "miniutility");
    }

    @EventHandler
    public void saveDeathLocation(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation().getBlock().getLocation().add(0, 1, 0);
        miniutility.setDeathLocation(player.getUniqueId(), deathLocation);
    }

    public static void teleportToDeathLocation(Player player, Location location) {
        if (player == null || location == null) {
            return;
        }

        try {
            player.teleportAsync(location);
        } catch (Throwable ignore) {
            player.teleport(location);
        }
    }

    public static Location getLastDeathLocation(Player player, Miniutility plugin) {
        if (player == null || plugin == null) {
            return null;
        }

        try {
            Location apiLocation = player.getLastDeathLocation();
            if (apiLocation != null) {
                return apiLocation.clone();
            }
        } catch (Throwable ignore) {
            // 非対応API時はフォールバックへ
        }

        return plugin.getDeathLocation(player.getUniqueId());
    }
}
