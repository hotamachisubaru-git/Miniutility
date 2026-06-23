package org.hotamachisubaru.miniutility.death;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DeathLocationStore {

    private final Map<UUID, Location> deathLocations = new HashMap<>();

    public void record(UUID playerId, Location location) {
        deathLocations.put(
                Objects.requireNonNull(playerId, "playerId"),
                Objects.requireNonNull(location, "location").clone()
        );
    }

    public Optional<Location> find(UUID playerId) {
        Location location = deathLocations.get(Objects.requireNonNull(playerId, "playerId"));
        return location == null ? Optional.empty() : Optional.of(location.clone());
    }
}
