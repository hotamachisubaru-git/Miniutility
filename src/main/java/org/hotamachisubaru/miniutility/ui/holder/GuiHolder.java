package org.hotamachisubaru.miniutility.ui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;
import java.util.Objects;

public final class GuiHolder implements InventoryHolder {

    private final GuiType type;
    private final UUID owner;
    private Inventory inventory;

    public GuiHolder(GuiType type, UUID owner) {
        this.type = Objects.requireNonNull(type, "type");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public GuiType getType() {
        return type;
    }

    public UUID getOwner() {
        return owner;
    }

    public void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("GUI inventory is already bound.");
        }
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    public Inventory getInventory() {
        return Objects.requireNonNull(inventory, "GUI inventory is not bound.");
    }
}
