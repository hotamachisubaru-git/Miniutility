package org.hotamachisubaru.miniutility.death;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerDeathLocationsTest {

    @Test
    void snapshotsAndResultsCannotChangeTheStoredCoordinates() {
        UUID worldId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        World world = world(worldId);
        PlayerDeathLocations store = new PlayerDeathLocations(id -> world);
        Location supplied = new Location(world, -1.5, 64, 20.25, 130, -15);
        store.record(playerId, supplied);

        supplied.setX(900);
        supplied.setYaw(0);
        Location first = store.find(playerId).orElseThrow();
        assertEquals(-1.5, first.getX());
        assertEquals(130, first.getYaw());
        first.setZ(900);
        Location next = store.find(playerId).orElseThrow();
        assertEquals(20.25, next.getZ());
        assertEquals(-15, next.getPitch());
        assertNotSame(first, next);
    }

    @Test
    void unloadedWorldDoesNotLoseTheDeathPointAndReloadUsesTheNewWorld() {
        UUID worldId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        World original = world(worldId);
        Map<UUID, World> worlds = new HashMap<>();
        worlds.put(worldId, original);
        PlayerDeathLocations store = new PlayerDeathLocations(worlds::get);
        store.record(playerId, new Location(original, 1, 65, 2));

        worlds.remove(worldId);
        assertTrue(store.find(playerId).isEmpty());

        World reloaded = world(worldId);
        worlds.put(worldId, reloaded);
        Location recovered = store.find(playerId).orElseThrow();
        assertSame(reloaded, recovered.getWorld());
        assertEquals(65, recovered.getY());
    }

    @Test
    void latestDeathReplacesPreviousDeathAndMissingPlayerIsEmpty() {
        UUID playerId = UUID.randomUUID();
        PlayerDeathLocations store = new PlayerDeathLocations(id -> null);
        assertTrue(store.find(playerId).isEmpty());
        store.record(playerId, new Location(null, 1, 2, 3));
        store.record(playerId, new Location(null, 4, 5, 6));
        Location latest = store.find(playerId).orElseThrow();
        assertNull(latest.getWorld());
        assertEquals(4, latest.getX());
        assertEquals(5, latest.getY());
        assertEquals(6, latest.getZ());
    }

    private static World world(UUID worldId) {
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldId);
        return world;
    }
}
