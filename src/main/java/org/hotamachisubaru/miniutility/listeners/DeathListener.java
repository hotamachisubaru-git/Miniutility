package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.hotamachisubaru.miniutility.death.DeathLocationStore;

import java.util.Objects;

public final class DeathListener implements Listener {

    private final DeathLocationStore deathLocationStore;

    public DeathListener(DeathLocationStore deathLocationStore) {
        this.deathLocationStore = Objects.requireNonNull(deathLocationStore, "deathLocationStore");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location currentLocation = player.getLocation();
        Location deathLocation = currentLocation.getBlock().getLocation().add(0, 1, 0);
        deathLocationStore.record(player.getUniqueId(), deathLocation);
    }
}
