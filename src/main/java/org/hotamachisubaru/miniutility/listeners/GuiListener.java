package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.ui.GuiActionService;
import org.hotamachisubaru.miniutility.ui.TrashBoxSessionStore;
import org.hotamachisubaru.miniutility.ui.holder.GuiHolder;
import org.hotamachisubaru.miniutility.ui.holder.GuiType;

import java.util.Objects;

public final class GuiListener implements Listener {

    private final GuiActionService guiActionService;
    private final TrashBoxSessionStore trashBoxSessionStore;

    public GuiListener(GuiActionService guiActionService, TrashBoxSessionStore trashBoxSessionStore) {
        this.guiActionService = Objects.requireNonNull(guiActionService, "guiActionService");
        this.trashBoxSessionStore = Objects.requireNonNull(trashBoxSessionStore, "trashBoxSessionStore");
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
        if (!guiHolder.getOwner().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != topInventory) {
            if (guiHolder.getType() != GuiType.TRASH) {
                event.setCancelled(true);
            }
            return;
        }

        boolean isTrashContentSlot = guiHolder.getType() == GuiType.TRASH
                && event.getRawSlot() < topInventory.getSize() - 1;
        event.setCancelled(!isTrashContentSlot);

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
            case TRASH -> {
                if (!isTrashContentSlot) {
                    guiActionService.handleTrashAction(player, topInventory, clickedItem);
                }
            }
            case TRASH_CONFIRM -> {
                event.setCancelled(true);
                guiActionService.handleTrashConfirmClick(player, clickedItem);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof GuiHolder guiHolder)) {
            return;
        }
        if (!guiHolder.getOwner().equals(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        int topSize = topInventory.getSize();
        boolean affectsTopInventory = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (!affectsTopInventory) {
            return;
        }

        boolean isValidTrashDrag = guiHolder.getType() == GuiType.TRASH
                && !event.getRawSlots().contains(topSize - 1);
        event.setCancelled(!isValidTrashDrag);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof GuiHolder guiHolder
                && guiHolder.getType() == GuiType.TRASH_CONFIRM) {
            trashBoxSessionStore.clear(guiHolder.getOwner());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        trashBoxSessionStore.clear(event.getPlayer().getUniqueId());
    }
}
