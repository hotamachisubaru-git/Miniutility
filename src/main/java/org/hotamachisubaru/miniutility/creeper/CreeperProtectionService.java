package org.hotamachisubaru.miniutility.creeper;

public final class CreeperProtectionService {

    private boolean enabled;

    public CreeperProtectionService(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
