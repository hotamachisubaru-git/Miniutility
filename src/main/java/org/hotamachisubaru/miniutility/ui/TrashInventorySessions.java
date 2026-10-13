package org.hotamachisubaru.miniutility.ui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.ui.holder.OwnedMenuInventoryHolder;
import org.hotamachisubaru.miniutility.ui.holder.UtilityMenuKind;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TrashInventorySessions {

    private final Map<UUID, @Nullable ItemStack[]> snapshots = new HashMap<>();

    public void save(UUID playerId, Inventory inventory) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(inventory, "inventory");
        if (!(inventory.getHolder(false) instanceof OwnedMenuInventoryHolder holder)
                || holder.getType() != UtilityMenuKind.TRASH
                || !holder.isOwner(playerId)
                || inventory.getSize() != UtilityMenuKind.TRASH.getSize()) {
            throw new IllegalArgumentException("Only the owner's trash inventory can be saved.");
        }
        snapshots.put(playerId, snapshotContents(inventory));
    }

    public @Nullable ItemStack[] remove(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        @Nullable ItemStack[] snapshot = snapshots.remove(playerId);
        return snapshot == null ? new @Nullable ItemStack[0] : snapshot;
    }

    public void clear(UUID playerId) {
        snapshots.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public void clear() {
        snapshots.clear();
    }

    private static @Nullable ItemStack[] snapshotContents(Inventory inventory) {
        @Nullable ItemStack[] snapshot = new @Nullable ItemStack[UtilityMenuKind.TRASH.getContentSlots()];
        for (int slot = 0; slot < snapshot.length; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null) {
                continue;
            }
            Material material = item.getType();
            snapshot[slot] = switch (material) {
                case AIR, CAVE_AIR, VOID_AIR -> null;
                default -> item.clone();
            };
        }
        return snapshot;
    }
}
