package org.hotamachisubaru.miniutility.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TrashBoxSessionStore {

    private static final int TRASH_CONTENT_SLOTS = 53;

    private final Map<UUID, ItemStack[]> snapshots = new HashMap<>();

    public void save(UUID playerId, Inventory inventory) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(inventory, "inventory");
        snapshots.put(playerId, snapshotContents(inventory));
    }

    public ItemStack[] remove(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        ItemStack[] snapshot = snapshots.remove(playerId);
        return snapshot == null ? new ItemStack[0] : snapshot;
    }

    public void clear(UUID playerId) {
        snapshots.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    private static ItemStack[] snapshotContents(Inventory inventory) {
        ItemStack[] snapshot = new ItemStack[TRASH_CONTENT_SLOTS];
        for (int slot = 0; slot < snapshot.length; slot++) {
            ItemStack item = inventory.getItem(slot);
            snapshot[slot] = item == null ? null : item.clone();
        }
        return snapshot;
    }
}
