package org.hotamachisubaru.miniutility.GUI;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrashBoxSessionStore {

    private static final int TRASH_CONTENT_SLOTS = 53;

    private final Map<UUID, ItemStack[]> snapshots = new ConcurrentHashMap<>();

    public void save(UUID uniqueId, Inventory inventory) {
        if (uniqueId == null || inventory == null) {
            return;
        }
        snapshots.put(uniqueId, snapshotContents(inventory));
    }

    public ItemStack[] remove(UUID uniqueId) {
        return snapshots.remove(uniqueId);
    }

    public void clear(UUID uniqueId) {
        snapshots.remove(uniqueId);
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
