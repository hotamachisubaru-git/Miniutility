package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.hotamachisubaru.miniutility.death.PlayerDeathLocations;

import java.util.Objects;

public final class PlayerDeathListener implements Listener {

    private final PlayerDeathLocations deathLocationStore;

    public PlayerDeathListener(PlayerDeathLocations deathLocationStore) {
        this.deathLocationStore = Objects.requireNonNull(deathLocationStore, "deathLocationStore");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event instanceof org.bukkit.event.Cancellable cancellable && cancellable.isCancelled()) {
            return;
        }
        Player player = event.getEntity();
        Location currentLocation = player.getLocation();
        // ブロック取得でチャンクを読み込まず、従来のブロック座標 + 1 と向き（yaw/pitch=0）を保持する。
        Location deathLocation = new Location(currentLocation.getWorld(), currentLocation.getBlockX(),
                currentLocation.getBlockY() + 1, currentLocation.getBlockZ());
        deathLocationStore.record(player.getUniqueId(), deathLocation);
    }
}
