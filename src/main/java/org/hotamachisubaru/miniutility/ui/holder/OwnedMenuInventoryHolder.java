package org.hotamachisubaru.miniutility.ui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.Objects;

public final class OwnedMenuInventoryHolder implements InventoryHolder {

    private final UtilityMenuKind type;
    private final UUID owner;
    private @Nullable Inventory inventory;

    public OwnedMenuInventoryHolder(UtilityMenuKind type, UUID owner) {
        this.type = Objects.requireNonNull(type, "type");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public UtilityMenuKind getType() {
        return type;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isOwner(UUID playerId) {
        return owner.equals(Objects.requireNonNull(playerId, "playerId"));
    }

    public void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("GUI inventory is already bound.");
        }
        Objects.requireNonNull(inventory, "inventory");
        if (inventory.getSize() != type.getSize() || inventory.getHolder(false) != this) {
            throw new IllegalArgumentException("Inventory does not belong to this GUI holder.");
        }
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("GUI inventory is not bound.");
        }
        return inventory;
    }
}
