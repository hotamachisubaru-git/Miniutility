package org.hotamachisubaru.miniutility.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.ui.holder.GuiHolder;
import org.hotamachisubaru.miniutility.ui.holder.GuiType;
import org.hotamachisubaru.miniutility.util.ComponentUtil;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GuiFactory {

    private static final int MENU_SIZE = 27;
    private static final int NICKNAME_MENU_SIZE = 9;
    private static final int TRASH_SIZE = 54;
    private static final int TRASH_CONFIRM_SIZE = 9;
    private static final int TRASH_ACTION_SLOT = 53;

    private GuiFactory() {
    }

    public static Inventory createMenu(UUID owner) {
        Inventory inventory = createInventory(GuiType.MENU, owner, MENU_SIZE, ComponentUtil.text("メニュー"));
        inventory.setItem(0, createMenuItem(Material.ARMOR_STAND, "死亡地点にワープ", "死亡地点にワープします。溺れたり、溶岩遊泳した場合は安全な場所にテレポートします。"));
        inventory.setItem(2, createMenuItem(Material.EXPERIENCE_BOTTLE, "経験値制御器", "経験値を制御します"));
        inventory.setItem(4, createMenuItem(Material.COMPASS, "ゲームモード制御器", "ゲームモードを制御します"));
        inventory.setItem(9, createMenuItem(Material.CREEPER_HEAD, "クリーパーのブロック破壊を防ぐ", "クリーパーのブロック破壊を防ぎます。有効/無効を切り替えられます。"));
        inventory.setItem(11, createMenuItem(Material.ENDER_CHEST, "エンダーチェスト", "クリックしてエンダーチェストを開く"));
        inventory.setItem(13, createMenuItem(Material.DROPPER, "ゴミ箱", "クリックしてゴミ箱を開く"));
        inventory.setItem(15, createMenuItem(Material.NAME_TAG, "ニックネームを変更", "クリックしてニックネームを変更"));
        inventory.setItem(17, createMenuItem(Material.CRAFTING_TABLE, "どこでも作業台", "クリックして作業台を開く"));
        return inventory;
    }

    public static Inventory createNicknameMenu(UUID owner) {
        Inventory inventory = createInventory(
                GuiType.NICKNAME,
                owner,
                NICKNAME_MENU_SIZE,
                ComponentUtil.text("ニックネームを変更")
        );
        inventory.setItem(2, createMenuItem(Material.PAPER, "ニックネーム入力", "クリックして新しいニックネームを入力"));
        inventory.setItem(4, createMenuItem(Material.NAME_TAG, "カラーコード指定", "クリックして色付きニックネームを入力"));
        inventory.setItem(6, createMenuItem(Material.BARRIER, "リセット", "ニックネームをリセット"));
        return inventory;
    }

    public static Inventory createTrashBox(UUID owner) {
        return createTrashBox(owner, new ItemStack[0]);
    }

    public static Inventory createTrashBox(UUID owner, ItemStack[] contents) {
        Objects.requireNonNull(contents, "contents");
        Inventory inventory = createInventory(GuiType.TRASH, owner, TRASH_SIZE, ComponentUtil.text("ゴミ箱"));
        for (int slot = 0; slot < Math.min(TRASH_ACTION_SLOT, contents.length); slot++) {
            ItemStack item = contents[slot];
            inventory.setItem(slot, item == null ? null : item.clone());
        }

        inventory.setItem(TRASH_ACTION_SLOT, createMenuItem(
                Material.LIME_WOOL,
                ComponentUtil.text("捨てる", NamedTextColor.RED),
                ComponentUtil.text("クリックして削除確認へ", NamedTextColor.GRAY)
        ));
        return inventory;
    }

    public static Inventory createTrashConfirm(UUID owner) {
        Inventory inventory = createInventory(
                GuiType.TRASH_CONFIRM,
                owner,
                TRASH_CONFIRM_SIZE,
                ComponentUtil.text("本当に捨てますか？")
        );
        inventory.setItem(3, createMenuItem(
                Material.LIME_WOOL,
                ComponentUtil.text("はい", NamedTextColor.GREEN),
                ComponentUtil.text("ゴミ箱を空にする", NamedTextColor.GRAY)
        ));
        inventory.setItem(5, createMenuItem(
                Material.RED_WOOL,
                ComponentUtil.text("いいえ", NamedTextColor.RED),
                ComponentUtil.text("キャンセル", NamedTextColor.GRAY)
        ));
        return inventory;
    }

    private static ItemStack createMenuItem(Material material, String name, String lore) {
        return createMenuItem(
                material,
                ComponentUtil.text(name, NamedTextColor.YELLOW),
                ComponentUtil.text(lore, NamedTextColor.GRAY)
        );
    }

    private static ItemStack createMenuItem(Material material, Component name, Component... lore) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");

        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(name);
            if (lore.length > 0) {
                meta.lore(List.of(lore));
            }
        });
        return item;
    }

    private static Inventory createInventory(GuiType type, UUID owner, int size, Component title) {
        GuiHolder holder = new GuiHolder(type, owner);
        Inventory inventory = Bukkit.createInventory(holder, size, Objects.requireNonNull(title, "title"));
        holder.bind(inventory);
        return inventory;
    }
}
