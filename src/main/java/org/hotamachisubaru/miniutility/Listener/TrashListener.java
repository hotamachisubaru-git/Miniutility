package org.hotamachisubaru.miniutility.Listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.GUI.holder.GuiHolder;
import org.hotamachisubaru.miniutility.GUI.holder.GuiType;
import org.hotamachisubaru.miniutility.MiniutilityLoader;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.hotamachisubaru.miniutility.GUI.GUI.createMenuItem;

public final class TrashListener implements Listener {

    private static final int TRASH_SIZE = 54;
    private static final int CONFIRM_BUTTON_SLOT = 53;

    private final Map<UUID, Inventory> lastTrashBox = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> trashBoxCache = new ConcurrentHashMap<>();
    public TrashListener(MiniutilityLoader plugin) {
        Objects.requireNonNull(plugin, "plugin");
    }

    private static Component colored(String message, NamedTextColor color) {
        return Component.text(message, color);
    }

    public void openTrashBox(Player player) {
        GuiHolder h = new GuiHolder(GuiType.TRASH, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(h, TRASH_SIZE, Component.text("ゴミ箱"));
        h.bind(inv);

        ItemStack confirm = createMenuItem(Material.LIME_WOOL,
                "捨てる",
                "クリックして削除確認へ"
        );
        inv.setItem(CONFIRM_BUTTON_SLOT, confirm);

        lastTrashBox.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    private void openTrashConfirm(Player player) {
        Inventory last = lastTrashBox.get(player.getUniqueId());
        if (last != null) {
            trashBoxCache.put(player.getUniqueId(), last.getContents());
        }

        GuiHolder h = new GuiHolder(GuiType.TRASH_CONFIRM, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(h, 9, Component.text("本当に捨てますか？"));
        h.bind(inv);

        inv.setItem(3, createMenuItem(
                Material.LIME_WOOL,
                "はい",
                "ゴミ箱を空にする"
        ));
        inv.setItem(5, createMenuItem(
                Material.RED_WOOL,
                "いいえ",
                "キャンセル"
        ));

        player.openInventory(inv);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrashClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof GuiHolder guiHolder)) {
            return;
        }

        if (guiHolder.getType() == GuiType.TRASH) {
            handleTrashInventoryClick(player, event);
        } else if (guiHolder.getType() == GuiType.TRASH_CONFIRM) {
            handleConfirmInventoryClick(player, event);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        cleanup(event.getPlayer().getUniqueId());
    }

    private void handleTrashInventoryClick(Player player, InventoryClickEvent event) {
        if (event.getRawSlot() != CONFIRM_BUTTON_SLOT) {
            event.setCancelled(false);
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.LIME_WOOL) {
            return;
        }

        event.setCancelled(true);
        openTrashConfirm(player);
    }

    private void handleConfirmInventoryClick(Player player, InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        if (item.getType() == Material.LIME_WOOL) {
            deleteAllTrashItems(player);
            return;
        }

        if (item.getType() == Material.RED_WOOL) {
            restoreTrashBox(player);
        }
    }

    private void deleteAllTrashItems(Player player) {
        Inventory previous = lastTrashBox.get(player.getUniqueId());
        if (previous != null) {
            for (int i = 0; i < CONFIRM_BUTTON_SLOT; i++) {
                previous.setItem(i, null);
            }
        }
        player.closeInventory();
        cleanup(player.getUniqueId());
        player.sendMessage(colored("ゴミ箱のアイテムをすべて削除しました。", NamedTextColor.GREEN));
    }

    private void restoreTrashBox(Player player) {
        ItemStack[] cache = trashBoxCache.remove(player.getUniqueId());

        GuiHolder holder = new GuiHolder(GuiType.TRASH, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, TRASH_SIZE, Component.text("ゴミ箱"));
        holder.bind(inventory);

        if (cache != null) {
            for (int i = 0; i < CONFIRM_BUTTON_SLOT && i < cache.length; i++) {
                inventory.setItem(i, cache[i]);
            }
        }
        inventory.setItem(CONFIRM_BUTTON_SLOT, createMenuItem(Material.LIME_WOOL, "捨てる", "クリックして削除確認へ"));

        lastTrashBox.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);
        player.sendMessage(colored("削除をキャンセルしました。", NamedTextColor.YELLOW));
    }

    private void cleanup(UUID uuid) {
        lastTrashBox.remove(uuid);
        trashBoxCache.remove(uuid);
    }
}
