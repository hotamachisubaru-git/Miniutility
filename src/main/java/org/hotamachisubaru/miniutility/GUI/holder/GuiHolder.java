package org.hotamachisubaru.miniutility.GUI.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;
import java.util.UUID;

public final class GuiHolder implements InventoryHolder {
    private Inventory inventory;
    private final GuiType type;
    private final UUID owner;

    public GuiHolder(GuiType type, UUID owner) {
        this.type = Objects.requireNonNull(type, "type");
        this.owner = owner;
    }

    public GuiType getType() {
        return type;
    }

    public UUID getOwner() {
        return owner;
    }

    public void bind(Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("GuiHolder が Inventory に紐付いていません。");
        }
        return inventory;
    }
}
