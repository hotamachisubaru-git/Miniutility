package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.ui.UtilityMenuActions;
import org.hotamachisubaru.miniutility.ui.TrashInventorySessions;
import org.hotamachisubaru.miniutility.ui.holder.OwnedMenuInventoryHolder;
import org.hotamachisubaru.miniutility.ui.holder.UtilityMenuKind;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

final class UtilityMenuListenerTest {

    private final UUID owner = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final Inventory top = mock(Inventory.class);
    private final Inventory bottom = mock(Inventory.class);
    private final InventoryView view = mock(InventoryView.class);
    private final UtilityMenuActions actions = mock(UtilityMenuActions.class);
    private final TrashInventorySessions sessions = mock(TrashInventorySessions.class);
    private final UtilityMenuListener listener = new UtilityMenuListener(actions, sessions);

    private void open(UtilityMenuKind type) {
        when(player.getUniqueId()).thenReturn(owner);
        when(view.getTopInventory()).thenReturn(top);
        when(top.getHolder()).thenReturn(new OwnedMenuInventoryHolder(type, owner));
        when(top.getSize()).thenReturn(type.getSize());
    }

    private InventoryClickEvent click(Inventory clickedInventory, int rawSlot, Material material) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getView()).thenReturn(view);
        when(event.getClickedInventory()).thenReturn(clickedInventory);
        when(event.getRawSlot()).thenReturn(rawSlot);
        when(event.getCurrentItem()).thenReturn(item);
        when(event.getAction()).thenReturn(InventoryAction.PICKUP_ALL);
        return event;
    }

    @Test
    void cancelsAButtonClickAndOnlySchedulesTheDeclaredMenuSlot() {
        open(UtilityMenuKind.MENU);
        InventoryClickEvent event = click(top, 17, Material.CRAFTING_TABLE);

        listener.onInventoryClick(event);

        verify(event).setCancelled(true);
        verify(actions).scheduleClick(player, top, 17);
        verify(actions, never()).handleMenuClick(any(), any());

        listener.onInventoryClick(click(top, 1, Material.CRAFTING_TABLE));
        verifyNoMoreInteractions(actions);
    }

    @Test
    void leavesTrashContentAndOrdinaryPlayerInventoryTransfersAvailable() {
        open(UtilityMenuKind.TRASH);
        InventoryClickEvent content = click(top, 52, Material.DIAMOND);
        InventoryClickEvent shift = click(bottom, 54, Material.DIAMOND);
        when(shift.getAction()).thenReturn(InventoryAction.MOVE_TO_OTHER_INVENTORY);

        listener.onInventoryClick(content);
        listener.onInventoryClick(shift);

        verify(content, never()).setCancelled(true);
        verify(shift, never()).setCancelled(true);
        verifyNoInteractions(actions);
    }

    @Test
    void preventsDoubleClickCollectionAndShiftMergingOfTheTrashButton() {
        open(UtilityMenuKind.TRASH);
        ItemStack button = mock(ItemStack.class);
        when(top.getItem(53)).thenReturn(button);
        InventoryClickEvent collect = click(bottom, 54, Material.LIME_WOOL);
        ItemStack cursor = mock(ItemStack.class);
        when(collect.getCursor()).thenReturn(cursor);
        when(collect.getAction()).thenReturn(InventoryAction.COLLECT_TO_CURSOR);
        when(cursor.isSimilar(button)).thenReturn(true);
        InventoryClickEvent shift = click(bottom, 54, Material.LIME_WOOL);
        when(shift.getAction()).thenReturn(InventoryAction.MOVE_TO_OTHER_INVENTORY);
        when(shift.getCurrentItem().isSimilar(button)).thenReturn(true);

        listener.onInventoryClick(collect);
        listener.onInventoryClick(shift);

        verify(collect).setCancelled(true);
        verify(shift).setCancelled(true);
        verifyNoInteractions(actions);
    }

    @Test
    void allowsTrashContentDragsButCancelsDragAcrossTheReservedButton() {
        open(UtilityMenuKind.TRASH);
        InventoryDragEvent content = drag(Set.of(0, 52, 54));
        InventoryDragEvent button = drag(Set.of(0, 53));

        listener.onInventoryDrag(content);
        listener.onInventoryDrag(button);

        verify(content).setCancelled(false);
        verify(button).setCancelled(true);
    }

    @Test
    void rejectsAnotherPlayerAndOnlyClosesTheOwnersConfirmationSession() {
        open(UtilityMenuKind.TRASH_CONFIRM);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        InventoryClickEvent click = click(top, 3, Material.LIME_WOOL);
        InventoryCloseEvent close = mock(InventoryCloseEvent.class);
        when(close.getInventory()).thenReturn(top);
        when(close.getPlayer()).thenReturn(player);

        listener.onInventoryClick(click);
        listener.onInventoryClose(close);

        verify(click).setCancelled(true);
        verifyNoInteractions(actions, sessions);
        when(player.getUniqueId()).thenReturn(owner);
        listener.onInventoryClose(close);
        verify(sessions).clear(owner);
    }

    @Test
    void releasesAPlayersConfirmationSnapshotOnQuit() {
        when(player.getUniqueId()).thenReturn(owner);
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);

        listener.onPlayerQuit(event);

        verify(sessions).clear(owner);
    }

    private InventoryDragEvent drag(Set<Integer> slots) {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlots()).thenReturn(slots);
        return event;
    }
}
