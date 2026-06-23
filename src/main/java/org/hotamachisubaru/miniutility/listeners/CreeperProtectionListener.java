package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;

import java.util.Objects;

public final class CreeperProtectionListener implements Listener {

    private final CreeperProtectionService creeperProtectionService;

    public CreeperProtectionListener(CreeperProtectionService creeperProtectionService) {
        this.creeperProtectionService = Objects.requireNonNull(
                creeperProtectionService,
                "creeperProtectionService"
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (creeperProtectionService.isEnabled() && event.getEntity() instanceof Creeper) {
            event.blockList().clear();
        }
    }
}
