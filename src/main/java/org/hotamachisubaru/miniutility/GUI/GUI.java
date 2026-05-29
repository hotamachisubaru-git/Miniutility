package org.hotamachisubaru.miniutility.GUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hotamachisubaru.miniutility.GUI.holder.GuiHolder;
import org.hotamachisubaru.miniutility.GUI.holder.GuiType;

import java.util.List;
import java.util.UUID;

public final class GUI {

    private GUI() {
    }

    public static Inventory createMenu(UUID owner) {
        Inventory inventory = createInventory(GuiType.MENU, owner, 27, Component.text("メニュー"));
        inventory.setItem(0, createMenuItem(Material.ARMOR_STAND, "死亡地点にワープ", "死亡地点にワープします。溺れたり、溶岩遊泳した場合は安全な場所にテレポートします。"));
        inventory.setItem(2, createMenuItem(Material.EXPERIENCE_BOTTLE, "経験値制御器", "経験値を制御します"));
        inventory.setItem(4, createMenuItem(Material.COMPASS, "ゲームモード制御器", "ゲームモードを制御します"));
        inventory.setItem(9, createMenuItem(Material.CREEPER_HEAD, "クリーパーのブロック破壊を防ぐ", "クリーパーのブロック破壊を防ぎます。ON/OFFができます。"));
        inventory.setItem(11, createMenuItem(Material.ENDER_CHEST, "エンダーチェスト", "クリックしてエンダーチェストを開く"));
        inventory.setItem(13, createMenuItem(Material.DROPPER, "ゴミ箱", "クリックしてゴミ箱を開く"));
        inventory.setItem(15, createMenuItem(Material.NAME_TAG, "ニックネームを変更", "クリックしてニックネームを変更"));
        inventory.setItem(17, createMenuItem(Material.CRAFTING_TABLE, "どこでも作業台", "クリックして作業台を開く"));
        return inventory;
    }

    public static Inventory createNicknameMenu(UUID owner) {
        Inventory inventory = createInventory(GuiType.NICKNAME, owner, 9, Component.text("ニックネームを変更"));
        inventory.setItem(2, createMenuItem(Material.PAPER, "ニックネーム入力", "クリックして新しいニックネームを入力"));
        inventory.setItem(4, createMenuItem(Material.NAME_TAG, "カラーコード指定", "クリックして色付きニックネームを入力"));
        inventory.setItem(6, createMenuItem(Material.BARRIER, "リセット", "ニックネームをリセット"));
        return inventory;
    }

    public static Inventory createTrashBox(UUID owner, ItemStack[] contents) {
        Inventory inventory = createInventory(GuiType.TRASH, owner, 54, Component.text("ゴミ箱"));

        if (contents != null) {
            for (int slot = 0; slot < Math.min(53, contents.length); slot++) {
                ItemStack item = contents[slot];
                inventory.setItem(slot, item == null ? null : item.clone());
            }
        }

        inventory.setItem(53, createMenuItem(
                Material.LIME_WOOL,
                Component.text("捨てる", NamedTextColor.RED),
                Component.text("クリックして削除確認へ", NamedTextColor.GRAY)
        ));
        return inventory;
    }

    public static Inventory createTrashConfirm(UUID owner) {
        Inventory inventory = createInventory(GuiType.TRASH_CONFIRM, owner, 9, Component.text("本当に捨てますか？"));
        inventory.setItem(3, createMenuItem(
                Material.LIME_WOOL,
                Component.text("はい", NamedTextColor.GREEN),
                Component.text("ゴミ箱を空にする", NamedTextColor.GRAY)
        ));
        inventory.setItem(5, createMenuItem(
                Material.RED_WOOL,
                Component.text("いいえ", NamedTextColor.RED),
                Component.text("キャンセル", NamedTextColor.GRAY)
        ));
        return inventory;
    }

    public static ItemStack createMenuItem(Material material, String name, String lore) {
        return createMenuItem(
                material,
                Component.text(name, NamedTextColor.YELLOW),
                Component.text(lore, NamedTextColor.GRAY)
        );
    }

    public static ItemStack createMenuItem(Material material, String name) {
        return createMenuItem(material, Component.text(name, NamedTextColor.YELLOW));
    }

    public static ItemStack createMenuItem(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(name);
            if (lore.length > 0) {
                meta.lore(List.of(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Inventory createInventory(GuiType type, UUID owner, int size, Component title) {
        GuiHolder holder = new GuiHolder(type, owner);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.bind(inventory);
        return inventory;
    }
}
