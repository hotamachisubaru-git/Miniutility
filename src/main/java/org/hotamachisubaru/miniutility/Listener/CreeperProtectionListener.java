package org.hotamachisubaru.miniutility.Listener;

import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CreeperProtectionListener implements Listener {
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (enabled.get() && event.getEntity() instanceof Creeper) {
            event.setCancelled(true);
        }
    }

    public boolean toggle() {
        return enabled.updateAndGet(current -> !current);
    }

    public boolean isEnabled() {
        return enabled.get();
    }
}
