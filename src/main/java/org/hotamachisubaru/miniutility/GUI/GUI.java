package org.hotamachisubaru.miniutility.GUI;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hotamachisubaru.miniutility.GUI.holder.GuiHolder;
import org.hotamachisubaru.miniutility.GUI.holder.GuiType;

import java.util.List;
import java.util.Objects;

public final class GUI {

    private GUI() {
    }

    public static void openMenu(Player player) {
        Objects.requireNonNull(player, "player");
        GuiHolder holder = new GuiHolder(GuiType.MENU, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, 27, "メニュー");
        holder.bind(inv);

        inv.setItem(0,  createMenuItem(Material.ARMOR_STAND,   "死亡地点にワープ",   "死亡地点にワープします。溺れたり、溶岩遊泳した場合は安全な場所にテレポートします。"));
        inv.setItem(2,  createMenuItem(Material.EXPERIENCE_BOTTLE, "経験値制御器",   "経験値を制御します"));
        inv.setItem(4,  createMenuItem(Material.COMPASS,       "ゲームモード制御器", "ゲームモードを制御します"));
        inv.setItem(9,  createMenuItem(Material.CREEPER_HEAD,  "クリーパーのブロック破壊を防ぐ", "クリーパーのブロック破壊を防ぎます。ON/OFFができます。"));
        inv.setItem(11, createMenuItem(Material.ENDER_CHEST,   "エンダーチェスト", "クリックしてエンダーチェストを開く"));
        inv.setItem(13, createMenuItem(Material.DROPPER,       "ゴミ箱",           "クリックしてゴミ箱を開く"));
        inv.setItem(15, createMenuItem(Material.NAME_TAG,      "ニックネームを変更", "クリックしてニックネームを変更"));
        inv.setItem(17, createMenuItem(Material.CRAFTING_TABLE,"どこでも作業台",   "クリックして作業台を開く"));

        player.openInventory(inv);
    }

    public static void openNicknameMenu(Player player) {
        Objects.requireNonNull(player, "player");
        GuiHolder holder = new GuiHolder(GuiType.NICKNAME, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, 9, "ニックネームを変更");
        holder.bind(inv);

        inv.setItem(2, createMenuItem(
                Material.PAPER,
                "ニックネーム入力",
                "クリックして新しいニックネームを入力"
        ));

        inv.setItem(4, createMenuItem(
                Material.NAME_TAG,
                "カラーコード指定",
                "クリックして色付きニックネームを入力"
        ));

        inv.setItem(6, createMenuItem(
                Material.BARRIER,
                "リセット",
                "ニックネームをリセット"
        ));

        player.openInventory(inv);
    }

    @Deprecated
    public static void NicknameMenu(Player player) {
        openNicknameMenu(player);
    }

    public static ItemStack createMenuItem(Material material, String name, String lore) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            meta.setLore(List.of(ChatColor.GRAY + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createMenuItem(Material material, String name) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
