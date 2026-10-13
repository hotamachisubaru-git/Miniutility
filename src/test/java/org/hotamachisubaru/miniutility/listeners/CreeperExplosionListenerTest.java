package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.hotamachisubaru.miniutility.creeper.CreeperBlockProtection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreeperExplosionListenerTest {

    @Test
    void protectedCreeperOnlyRemovesBlocksAndLeavesExplosionActive() {
        EntityExplodeEvent event = mock(EntityExplodeEvent.class);
        List<Block> blocks = new ArrayList<>();
        blocks.add(mock(Block.class));
        when(event.getEntity()).thenReturn(mock(Creeper.class));
        when(event.blockList()).thenReturn(blocks);

        new CreeperExplosionListener(new CreeperBlockProtection(true)).onCreeperExplode(event);

        assertTrue(blocks.isEmpty());
        verify(event, never()).setCancelled(anyBoolean());
        verify(event, never()).setYield(anyFloat());
    }

    @Test
    void disabledProtectionAllowsCreeperBlocksAndToggleRestoresProtection() {
        EntityExplodeEvent event = mock(EntityExplodeEvent.class);
        List<Block> blocks = new ArrayList<>();
        blocks.add(mock(Block.class));
        when(event.getEntity()).thenReturn(mock(Creeper.class));
        when(event.blockList()).thenReturn(blocks);
        CreeperBlockProtection service = new CreeperBlockProtection(false);
        CreeperExplosionListener listener = new CreeperExplosionListener(service);

        listener.onCreeperExplode(event);
        assertEquals(1, blocks.size());
        assertTrue(service.toggle());
        listener.onCreeperExplode(event);
        assertTrue(blocks.isEmpty());
    }

    @Test
    void otherExplosionSourcesAndCancelledEventsRemainUntouched() {
        EntityExplodeEvent tnt = mock(EntityExplodeEvent.class);
        when(tnt.getEntity()).thenReturn(mock(TNTPrimed.class));
        EntityExplodeEvent cancelled = mock(EntityExplodeEvent.class);
        when(cancelled.isCancelled()).thenReturn(true);
        CreeperExplosionListener listener = new CreeperExplosionListener(new CreeperBlockProtection(true));

        listener.onCreeperExplode(tnt);
        listener.onCreeperExplode(cancelled);

        verify(tnt, never()).blockList();
        verify(cancelled, never()).blockList();
        verify(cancelled, never()).setCancelled(anyBoolean());
    }
}
