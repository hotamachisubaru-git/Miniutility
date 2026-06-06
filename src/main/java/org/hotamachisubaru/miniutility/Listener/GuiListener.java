package org.hotamachisubaru.miniutility.Listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.GUI.GuiActionService;
import org.hotamachisubaru.miniutility.GUI.TrashBoxSessionStore;
import org.hotamachisubaru.miniutility.GUI.holder.GuiHolder;

public final class GuiListener implements Listener {

    private final GuiActionService guiActionService;
    private final TrashBoxSessionStore trashBoxSessionStore;

    public GuiListener(GuiActionService guiActionService, TrashBoxSessionStore trashBoxSessionStore) {
        this.guiActionService = guiActionService;
        this.trashBoxSessionStore = trashBoxSessionStore;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof GuiHolder guiHolder)) {
            return;
        }
        if (event.getClickedInventory() != topInventory) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        switch (guiHolder.getType()) {
            case MENU -> {
                event.setCancelled(true);
                guiActionService.handleMenuClick(player, clickedItem);
            }
            case NICKNAME -> {
                event.setCancelled(true);
                guiActionService.handleNicknameClick(player, clickedItem);
            }
            case TRASH -> event.setCancelled(guiActionService.handleTrashClick(
                    player,
                    topInventory,
                    clickedItem,
                    event.getRawSlot()
            ));
            case TRASH_CONFIRM -> {
                event.setCancelled(true);
                guiActionService.handleTrashConfirmClick(player, clickedItem);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        trashBoxSessionStore.clear(event.getPlayer().getUniqueId());
    }
}
