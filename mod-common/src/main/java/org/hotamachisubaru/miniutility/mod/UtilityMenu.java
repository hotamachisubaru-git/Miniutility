package org.hotamachisubaru.miniutility.mod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class UtilityMenu extends CompatMenu {
    public enum Page {
        MAIN(3, "メニュー"), NICKNAME(1, "ニックネーム変更"), CONFIRM(1, "本当に捨てますか？");
        final int rows; final String title;
        Page(int rows, String title) { this.rows = rows; this.title = title; }
    }
    private final ModRuntime runtime;
    private final ServerPlayer owner;
    private final Page page;
    private final TrashSession trash;
    private boolean transfer;

    public UtilityMenu(int id, Inventory inventory, ModRuntime runtime, ServerPlayer owner, Page page, TrashSession trash) {
        super(page.rows == 3 ? MenuType.GENERIC_9x3 : MenuType.GENERIC_9x1, id);
        this.runtime = runtime; this.owner = owner; this.page = page; this.trash = trash;
        SimpleContainer icons = new SimpleContainer(page.rows * 9);
        for (int i = 0; i < page.rows * 9; i++) {
            final int index = i;
            addSlot(new Slot(icons, index, 8 + (i % 9) * 18, 18 + (i / 9) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        addPlayerSlots(inventory, page.rows);
        if (page == Page.MAIN) {
            icon(icons, 0, Items.ARMOR_STAND, "死亡地点にワープ");
            icon(icons, 2, Items.EXPERIENCE_BOTTLE, "経験値レベルを変更");
            icon(icons, 4, Items.COMPASS, "ゲームモード切替");
            icon(icons, 9, Items.CREEPER_HEAD, "クリーパーのブロック破壊防止");
            icon(icons, 11, Items.ENDER_CHEST, "エンダーチェスト");
            icon(icons, 13, Items.DROPPER, "ゴミ箱");
            icon(icons, 15, Items.NAME_TAG, "ニックネーム変更");
            icon(icons, 17, Items.CRAFTING_TABLE, "どこでも作業台");
        } else if (page == Page.NICKNAME) {
            icon(icons, 2, Items.PAPER, "ニックネーム入力");
            icon(icons, 4, Items.NAME_TAG, "カラーコード指定");
            icon(icons, 6, Items.BARRIER, "リセット");
        } else {
            icon(icons, 3, Items.EMERALD, "はい、削除します");
            icon(icons, 5, Items.BARRIER, "いいえ、戻ります");
        }
    }
    private static void icon(SimpleContainer icons, int slot, Item item, String name) {
        ItemStack stack = new ItemStack(item);
        Compat.label(stack, Component.literal(name));
        icons.setItem(slot, stack);
    }
    // AbstractContainerMenu.addSlot is protected, so each menu owns the player-slot registration.
    private void addPlayerSlots(Inventory inventory, int rows) {
        for (int y = 0; y < 3; y++) for (int x = 0; x < 9; x++)
            addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 31 + rows * 18 + y * 18));
        for (int x = 0; x < 9; x++) addSlot(new Slot(inventory, x, 8 + x * 18, 89 + rows * 18));
    }
    @Override public boolean stillValid(Player player) { return player == owner; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override protected boolean interceptClick(int slot, int button, boolean pickup, Player player) {
        if (player != owner || !pickup || button < 0 || button > 1
                || slot < 0 || slot >= page.rows * 9 || !slots.get(slot).hasItem()) return true;
        runtime.schedule(owner, this, () -> {
            if (page != Page.CONFIRM) runtime.choose(owner, page, slot);
            else if (trash != null && slot == 3) {
                trash.discard(); owner.closeContainer(); ModRuntime.tell(owner, "ゴミ箱のアイテムを削除しました。");
            } else if (trash != null && slot == 5) {
                transfer = true;
                try { if (!runtime.openTrash(owner, trash)) { transfer = false; trash.giveBack(owner); } }
                catch (RuntimeException failure) { transfer = false; trash.giveBack(owner); throw failure; }
            }
        });
        return true;
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (trash != null && !transfer) trash.giveBack(owner);
    }
}
