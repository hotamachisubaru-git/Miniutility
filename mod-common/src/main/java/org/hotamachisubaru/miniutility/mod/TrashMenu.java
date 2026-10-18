package org.hotamachisubaru.miniutility.mod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TrashMenu extends CompatMenu {
    private final ModRuntime runtime;
    private final ServerPlayer owner;
    private final TrashSession session;
    private boolean transfer;
    public TrashMenu(int id, Inventory inventory, ModRuntime runtime, ServerPlayer owner, TrashSession session) {
        super(MenuType.GENERIC_9x6, id);
        this.runtime = runtime; this.owner = owner; this.session = session;
        for (int i = 0; i < 53; i++) addSlot(new Slot(session.contents, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        addSlot(new Slot(session.contents, 53, 152, 108) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
        ItemStack confirm = new ItemStack(Items.EMERALD);
        Compat.label(confirm, Component.literal("削除を確認"));
        session.contents.setItem(53, confirm);
        for (int y = 0; y < 3; y++) for (int x = 0; x < 9; x++)
            addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 139 + y * 18));
        for (int x = 0; x < 9; x++) addSlot(new Slot(inventory, x, 8 + x * 18, 197));
    }
    @Override public boolean stillValid(Player player) { return player == owner; }
    @Override protected boolean interceptClick(int slot, int button, boolean pickup, Player player) {
        if (player != owner || transfer) return true;
        if (slot == 53) {
            if (pickup && (button == 0 || button == 1)) {
                runtime.schedule(owner, this, () -> {
                    transfer = true;
                    try { if (!runtime.confirmTrash(owner, session)) { transfer = false; session.giveBack(owner); } }
                    catch (RuntimeException failure) { transfer = false; session.giveBack(owner); throw failure; }
                });
            }
            return true;
        }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (player != owner || index < 0 || index >= slots.size() || index == 53) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 53 ? !moveItemStackTo(stack, 54, slots.size(), true) : !moveItemStackTo(stack, 0, 53, false))
            return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (!transfer) session.giveBack(owner);
    }
}
