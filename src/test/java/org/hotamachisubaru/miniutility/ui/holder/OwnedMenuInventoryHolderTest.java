package org.hotamachisubaru.miniutility.ui.holder;

import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class OwnedMenuInventoryHolderTest {

    @Test
    void cannotExposeAnUnboundInventoryOrBindAnotherHoldersInventory() {
        UUID owner = UUID.randomUUID();
        OwnedMenuInventoryHolder holder = new OwnedMenuInventoryHolder(UtilityMenuKind.MENU, owner);
        Inventory unrelated = mock(Inventory.class);
        when(unrelated.getSize()).thenReturn(UtilityMenuKind.MENU.getSize());

        assertThrows(IllegalStateException.class, holder::getInventory);
        assertThrows(IllegalArgumentException.class, () -> holder.bind(unrelated));
        assertTrue(holder.isOwner(owner));
        assertFalse(holder.isOwner(UUID.randomUUID()));
    }

    @Test
    void bindsExactlyOnceWithTheDeclaredInventorySize() {
        OwnedMenuInventoryHolder holder = new OwnedMenuInventoryHolder(UtilityMenuKind.TRASH, UUID.randomUUID());
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(holder);
        when(inventory.getSize()).thenReturn(9);
        assertThrows(IllegalArgumentException.class, () -> holder.bind(inventory));

        when(inventory.getSize()).thenReturn(54);
        holder.bind(inventory);

        assertSame(inventory, holder.getInventory());
        assertThrows(IllegalStateException.class, () -> holder.bind(inventory));
    }
}
