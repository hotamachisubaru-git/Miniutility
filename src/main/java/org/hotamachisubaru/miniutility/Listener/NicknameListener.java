package org.hotamachisubaru.miniutility.Listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hotamachisubaru.miniutility.GUI.holder.GuiHolder;
import org.hotamachisubaru.miniutility.GUI.holder.GuiType;
import org.hotamachisubaru.miniutility.Nickname.NicknameDatabase;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;

import java.util.List;

public class NicknameListener implements Listener {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public NicknameListener() {
    }

    private static Component colored(String message, NamedTextColor color) {
        return Component.text(message, color);
    }

    @EventHandler(ignoreCancelled = true)
    public void onNicknameMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        InventoryHolder holder = inv.getHolder();
        if (!(holder instanceof GuiHolder)) return;
        if (((GuiHolder) holder).getType() != GuiType.NICKNAME) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (item.getType() == Material.PAPER) {
            player.sendMessage(colored("新しいニックネームをチャットに入力してください。", NamedTextColor.AQUA));
            Chat.setWaitingForNickname(player, true);
            player.closeInventory();
        } else if (item.getType() == Material.NAME_TAG) {
            player.sendMessage(colored("色付きのニックネームをチャットで入力してください。例: &6ほたまち", NamedTextColor.AQUA));
            Chat.setWaitingForColorInput(player, true);
            player.closeInventory();
        } else if (item.getType() == Material.BARRIER) {
            NicknameDatabase.deleteNickname(player);
            NicknameManager.updateDisplayName(player);
            player.sendMessage(colored("ニックネームをリセットしました。", NamedTextColor.GREEN));
            player.closeInventory();
        } else {
            player.sendMessage(colored("無効な選択です。", NamedTextColor.RED));
        }
    }


    public static void openNicknameMenu(Player player) {
        GuiHolder holder = new GuiHolder(GuiType.NICKNAME, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("ニックネームを変更"));
        holder.bind(inv);

        inv.setItem(2, createMenuItem(Material.PAPER, "§eニックネームを入力", "§7クリックして新しいニックネームを入力"));
        inv.setItem(4, createMenuItem(Material.NAME_TAG, "§eカラーコード指定", "§7クリックして色付きニックネームを入力"));
        inv.setItem(6, createMenuItem(Material.BARRIER, "§eリセット", "§7ニックネームをリセット"));

        player.openInventory(inv);
    }

    private static ItemStack createMenuItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY_SERIALIZER.deserialize(name));
            meta.lore(List.of(LEGACY_SERIALIZER.deserialize(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
