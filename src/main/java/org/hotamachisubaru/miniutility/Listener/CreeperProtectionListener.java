package org.hotamachisubaru.miniutility.Listener;

import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;

public final class CreeperProtectionListener implements Listener {

    private final CreeperProtectionService creeperProtectionService;

    public CreeperProtectionListener(CreeperProtectionService creeperProtectionService) {
        this.creeperProtectionService = creeperProtectionService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (creeperProtectionService.isEnabled() && event.getEntity() instanceof Creeper) {
            event.setCancelled(true);
        }
    }
}
