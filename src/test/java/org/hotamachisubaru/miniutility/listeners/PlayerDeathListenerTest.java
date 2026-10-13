package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.hotamachisubaru.miniutility.death.PlayerDeathLocations;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerDeathListenerTest {

    @Test
    void recordsFlooredCoordinatesOneBlockAboveDeathWithoutReadingABlock() {
        UUID playerId = UUID.randomUUID();
        World world = mock(World.class);
        Location current = spy(new Location(world, -1.25, -5.25, 2.75, 95, 20));
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getLocation()).thenReturn(current);
        PlayerDeathEvent event = mock(PlayerDeathEvent.class);
        when(event.getEntity()).thenReturn(player);
        PlayerDeathLocations store = mock(PlayerDeathLocations.class);
        List<Location> recordedLocations = new ArrayList<>();
        doAnswer(invocation -> {
            recordedLocations.add(invocation.getArgument(1, Location.class));
            return null;
        }).<PlayerDeathLocations>when(store).record(eq(playerId), any(Location.class));

        new PlayerDeathListener(store).onPlayerDeath(event);

        verify(store).record(eq(playerId), any(Location.class));
        assertEquals(1, recordedLocations.size());
        Location saved = recordedLocations.getFirst();
        assertSame(world, saved.getWorld());
        assertEquals(-2, saved.getX());
        assertEquals(-5, saved.getY());
        assertEquals(2, saved.getZ());
        assertEquals(0, saved.getYaw());
        assertEquals(0, saved.getPitch());
        verify(current, never()).getBlock();
        assertEquals(-5.25, current.getY());
    }

    @Test
    void cancelledDeathDoesNotReplaceTheStoredDeathPoint() {
        PlayerDeathEvent event = mock(PlayerDeathEvent.class);
        when(event.isCancelled()).thenReturn(true);
        PlayerDeathLocations store = mock(PlayerDeathLocations.class);

        new PlayerDeathListener(store).onPlayerDeath(event);

        verifyNoInteractions(store);
        verify(event, never()).getEntity();
    }
}
