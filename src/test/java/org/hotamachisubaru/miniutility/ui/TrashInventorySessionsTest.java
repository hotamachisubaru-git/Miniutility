package org.hotamachisubaru.miniutility.ui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.ui.holder.OwnedMenuInventoryHolder;
import org.hotamachisubaru.miniutility.ui.holder.UtilityMenuKind;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class TrashInventorySessionsTest {

    @Test
    void savesIndependentContentWithoutTheDiscardButtonAndConsumesItOnce() {
        UUID owner = UUID.randomUUID();
        Inventory inventory = trashInventory(owner);
        ItemStack original = mock(ItemStack.class);
        ItemStack copy = mock(ItemStack.class);
        when(original.getType()).thenReturn(Material.DIAMOND);
        when(original.clone()).thenReturn(copy);
        when(inventory.getItem(0)).thenReturn(original);
        TrashInventorySessions store = new TrashInventorySessions();

        store.save(owner, inventory);
        @Nullable ItemStack[] snapshot = store.remove(owner);

        assertEquals(53, snapshot.length);
        assertSame(copy, snapshot[0]);
        assertNotSame(original, snapshot[0]);
        assertNull(snapshot[52]);
        verify(inventory, never()).getItem(53);
        assertEquals(0, store.remove(owner).length);
    }

    @Test
    void refusesToOverwriteASessionWithAnotherOwnersInventory() {
        UUID owner = UUID.randomUUID();
        TrashInventorySessions store = new TrashInventorySessions();
        store.save(owner, trashInventory(owner));

        assertThrows(IllegalArgumentException.class, () -> store.save(owner, trashInventory(UUID.randomUUID())));
        assertEquals(53, store.remove(owner).length);
    }

    @Test
    void treatsAllThreeAirMaterialsAsEmptyWithoutCloningThem() {
        UUID owner = UUID.randomUUID();
        Inventory inventory = trashInventory(owner);
        Material[] airMaterials = {Material.AIR, Material.CAVE_AIR, Material.VOID_AIR};
        ItemStack[] airItems = new ItemStack[airMaterials.length];
        for (int slot = 0; slot < airMaterials.length; slot++) {
            ItemStack item = mock(ItemStack.class);
            when(item.getType()).thenReturn(airMaterials[slot]);
            when(inventory.getItem(slot)).thenReturn(item);
            airItems[slot] = item;
        }
        TrashInventorySessions store = new TrashInventorySessions();
        store.save(owner, inventory);

        ItemStack[] snapshot = store.remove(owner);

        for (int slot = 0; slot < airItems.length; slot++) {
            assertNull(snapshot[slot]);
            verify(airItems[slot], never()).clone();
        }
    }

    @Test
    void clearsOnlyTheRequestedPlayerAndCanReleaseAllSessionsOnShutdown() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        TrashInventorySessions store = new TrashInventorySessions();
        store.save(first, trashInventory(first));
        store.save(second, trashInventory(second));

        store.clear(first);

        assertEquals(0, store.remove(first).length);
        assertEquals(53, store.remove(second).length);
        store.save(second, trashInventory(second));
        store.clear();
        assertEquals(0, store.remove(second).length);
    }

    private static Inventory trashInventory(UUID owner) {
        Inventory inventory = mock(Inventory.class);
        when(inventory.getSize()).thenReturn(54);
        when(inventory.getHolder()).thenReturn(new OwnedMenuInventoryHolder(UtilityMenuKind.TRASH, owner));
        return inventory;
    }
}
