package org.hotamachisubaru.miniutility.Listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.hotamachisubaru.miniutility.MiniutilityLoader;

public final class DeathListener implements Listener {

    private final MiniutilityLoader plugin;

    public DeathListener(MiniutilityLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location currentLocation = player.getLocation();
        if (currentLocation == null) {
            return;
        }

        Location deathLocation = currentLocation.getBlock().getLocation().add(0, 1, 0);
        plugin.recordDeathLocation(player.getUniqueId(), deathLocation);
    }
}
