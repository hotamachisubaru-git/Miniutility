package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.ui.UtilityMenuActions;
import org.hotamachisubaru.miniutility.ui.TrashInventorySessions;
import org.hotamachisubaru.miniutility.ui.holder.OwnedMenuInventoryHolder;
import org.hotamachisubaru.miniutility.ui.holder.UtilityMenuKind;
import org.hotamachisubaru.miniutility.ui.item.MenuButtonSpec;

import java.util.Objects;

public final class UtilityMenuListener implements Listener {

    private final UtilityMenuActions guiActionService;
    private final TrashInventorySessions trashBoxSessionStore;

    public UtilityMenuListener(UtilityMenuActions guiActionService, TrashInventorySessions trashBoxSessionStore) {
        this.guiActionService = Objects.requireNonNull(guiActionService, "guiActionService");
        this.trashBoxSessionStore = Objects.requireNonNull(trashBoxSessionStore, "trashBoxSessionStore");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof OwnedMenuInventoryHolder guiHolder)) {
            return;
        }
        if (!guiHolder.isOwner(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        UtilityMenuKind type = guiHolder.getType();
        if (type == UtilityMenuKind.TRASH && wouldMoveTrashButton(event, topInventory)) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != topInventory) {
            if (type != UtilityMenuKind.TRASH) {
                event.setCancelled(true);
            }
            return;
        }

        if (type.isContentSlot(event.getRawSlot())) {
            return;
        }
        event.setCancelled(true);

        MenuButtonSpec button = type.getButton(event.getRawSlot());
        if (button != null && button.matches(event.getCurrentItem())) {
            guiActionService.scheduleClick(player, topInventory, event.getRawSlot());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof OwnedMenuInventoryHolder guiHolder)) {
            return;
        }
        if (!guiHolder.isOwner(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        int topSize = topInventory.getSize();
        boolean affectsTopInventory = false;
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot == null) {
                event.setCancelled(true);
                return;
            }
            if (rawSlot < 0 || rawSlot >= topSize) {
                continue;
            }
            affectsTopInventory = true;
            if (!guiHolder.getType().isContentSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
        if (affectsTopInventory) {
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder(false) instanceof OwnedMenuInventoryHolder guiHolder
                && guiHolder.getType() == UtilityMenuKind.TRASH_CONFIRM
                && guiHolder.isOwner(event.getPlayer().getUniqueId())) {
            trashBoxSessionStore.clear(guiHolder.getOwner());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        trashBoxSessionStore.clear(event.getPlayer().getUniqueId());
    }

    private static boolean wouldMoveTrashButton(InventoryClickEvent event, Inventory inventory) {
        ItemStack button = inventory.getItem(UtilityMenuKind.TRASH.getContentSlots());
        if (button == null) {
            return false;
        }
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            return event.getCursor().isSimilar(button);
        }
        ItemStack clicked = event.getCurrentItem();
        return event.getClickedInventory() != inventory
                && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && clicked != null
                && clicked.isSimilar(button);
    }
}
