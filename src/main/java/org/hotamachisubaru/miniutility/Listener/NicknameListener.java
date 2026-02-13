package org.hotamachisubaru.miniutility.Listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.GUI.GUI;
import org.hotamachisubaru.miniutility.GUI.holder.GuiHolder;
import org.hotamachisubaru.miniutility.GUI.holder.GuiType;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;

import java.util.Objects;

public final class NicknameListener implements Listener {

    private final Chat chatListener;
    private final NicknameManager nicknameManager;

    public NicknameListener(Chat chatListener, NicknameManager nicknameManager) {
        this.chatListener = Objects.requireNonNull(chatListener, "chatListener");
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
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
            chatListener.beginNicknameInput(player);
            player.closeInventory();
        } else if (item.getType() == Material.NAME_TAG) {
            player.sendMessage(colored("色付きのニックネームをチャットで入力してください。例: &6ほたまち", NamedTextColor.AQUA));
            chatListener.beginColorNicknameInput(player);
            player.closeInventory();
        } else if (item.getType() == Material.BARRIER) {
            nicknameManager.removeNickname(player);
            player.sendMessage(colored("ニックネームをリセットしました。", NamedTextColor.GREEN));
            player.closeInventory();
        } else {
            player.sendMessage(colored("無効な選択です。", NamedTextColor.RED));
        }
    }

    public static void openNicknameMenu(Player player) {
        GUI.openNicknameMenu(player);
    }
}
