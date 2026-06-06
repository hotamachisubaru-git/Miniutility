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
import org.hotamachisubaru.miniutility.util.ComponentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class GUI {

    private GUI() {
    }

    public static @NotNull Inventory createMenu(@Nullable UUID owner) {
        Inventory inventory = createInventory(GuiType.MENU, owner, 27, ComponentUtil.text("メニュー"));
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

    public static @NotNull Inventory createNicknameMenu(@Nullable UUID owner) {
        Inventory inventory = createInventory(GuiType.NICKNAME, owner, 9, ComponentUtil.text("ニックネームを変更"));
        inventory.setItem(2, createMenuItem(Material.PAPER, "ニックネーム入力", "クリックして新しいニックネームを入力"));
        inventory.setItem(4, createMenuItem(Material.NAME_TAG, "カラーコード指定", "クリックして色付きニックネームを入力"));
        inventory.setItem(6, createMenuItem(Material.BARRIER, "リセット", "ニックネームをリセット"));
        return inventory;
    }

    public static @NotNull Inventory createTrashBox(@Nullable UUID owner, @Nullable ItemStack[] contents) {
        Inventory inventory = createInventory(GuiType.TRASH, owner, 54, ComponentUtil.text("ゴミ箱"));

        if (contents != null) {
            for (int slot = 0; slot < Math.min(53, contents.length); slot++) {
                ItemStack item = contents[slot];
                inventory.setItem(slot, item == null ? null : item.clone());
            }
        }

        inventory.setItem(53, createMenuItem(
                Material.LIME_WOOL,
                ComponentUtil.text("捨てる", NamedTextColor.RED),
                ComponentUtil.text("クリックして削除確認へ", NamedTextColor.GRAY)
        ));
        return inventory;
    }

    public static @NotNull Inventory createTrashConfirm(@Nullable UUID owner) {
        Inventory inventory = createInventory(GuiType.TRASH_CONFIRM, owner, 9, ComponentUtil.text("本当に捨てますか？"));
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

    public static @NotNull ItemStack createMenuItem(@NotNull Material material, @NotNull String name, @NotNull String lore) {
        return createMenuItem(
                material,
                ComponentUtil.text(name, NamedTextColor.YELLOW),
                ComponentUtil.text(lore, NamedTextColor.GRAY)
        );
    }

    public static @NotNull ItemStack createMenuItem(@NotNull Material material, @NotNull String name) {
        return createMenuItem(material, ComponentUtil.text(name, NamedTextColor.YELLOW));
    }

    public static @NotNull ItemStack createMenuItem(@NotNull Material material, @NotNull Component name, Component @NotNull ... lore) {
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

    private static @NotNull Inventory createInventory(@NotNull GuiType type, @Nullable UUID owner, int size, @NotNull Component title) {
        GuiHolder holder = new GuiHolder(type, owner);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.bind(inventory);
        return inventory;
    }
}
