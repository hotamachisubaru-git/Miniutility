package org.hotamachisubaru.miniutility.death;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeathLocationStore {

    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    public void record(UUID uniqueId, Location location) {
        if (uniqueId == null || location == null) {
            return;
        }
        deathLocations.put(uniqueId, location.clone());
    }

    public Location get(UUID uniqueId) {
        Location location = deathLocations.get(uniqueId);
        return location == null ? null : location.clone();
    }
}
