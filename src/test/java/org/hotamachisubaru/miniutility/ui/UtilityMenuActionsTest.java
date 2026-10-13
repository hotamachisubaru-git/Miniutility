package org.hotamachisubaru.miniutility.ui;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.hotamachisubaru.miniutility.creeper.CreeperBlockProtection;
import org.hotamachisubaru.miniutility.death.PlayerDeathLocations;
import org.hotamachisubaru.miniutility.listeners.PlayerChatListener;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.ui.holder.OwnedMenuInventoryHolder;
import org.hotamachisubaru.miniutility.ui.holder.UtilityMenuKind;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class UtilityMenuActionsTest {

    @Test
    void defersAndCoalescesButtonActionsUntilTheInventoryTransactionCompletes() {
        try (Fixture fixture = new Fixture()) {
            fixture.actions.scheduleClick(fixture.player, fixture.inventory, 4);
            fixture.actions.scheduleClick(fixture.player, fixture.inventory, 4);

            assertEquals(1, fixture.tasks.size());
            verifyNoGameModeChanges(fixture.player);
            fixture.runNextTask();

            verify(fixture.player).setGameMode(GameMode.CREATIVE);
            verify(fixture.player).closeInventory();
        }
    }

    @Test
    void cancelsStaleActionsWhenThePlayerChangesViewsOrDisconnects() {
        try (Fixture fixture = new Fixture()) {
            fixture.actions.scheduleClick(fixture.player, fixture.inventory, 4);
            Inventory replacement = mock(Inventory.class);
            when(fixture.view.getTopInventory()).thenReturn(replacement);
            fixture.runNextTask();
            verifyNoGameModeChanges(fixture.player);

            when(fixture.view.getTopInventory()).thenReturn(fixture.inventory);
            fixture.actions.scheduleClick(fixture.player, fixture.inventory, 4);
            when(fixture.player.isOnline()).thenReturn(false);
            fixture.runNextTask();
            verifyNoGameModeChanges(fixture.player);
        }
    }

    @Test
    void rejectsInvalidSlotsAndAnotherPlayersOwnedView() {
        try (Fixture fixture = new Fixture()) {
            fixture.actions.scheduleClick(fixture.player, fixture.inventory, -1);
            fixture.actions.scheduleClick(fixture.player, fixture.inventory, 1);
            when(fixture.inventory.getHolder(false)).thenReturn(new OwnedMenuInventoryHolder(UtilityMenuKind.MENU, UUID.randomUUID()));
            fixture.actions.scheduleClick(fixture.player, fixture.inventory, 4);

            assertTrue(fixture.tasks.isEmpty());
        }
    }

    @Test
    void cancellingTrashConfirmationRestoresTheSavedContentsBeforeClosingTheOldView() {
        try (Fixture fixture = new Fixture(); var factory = mockStatic(UtilityMenuFactory.class)) {
            @Nullable ItemStack[] snapshot = {mock(ItemStack.class)};
            Inventory restored = mock(Inventory.class);
            UUID owner = Objects.requireNonNull(fixture.owner);
            when(fixture.sessions.remove(fixture.owner)).thenReturn(snapshot);
            factory.when(() -> UtilityMenuFactory.createTrashBox(owner, snapshot)).thenReturn(restored);

            fixture.actions.handleTrashConfirmClick(fixture.player, item(Material.RED_WOOL));

            var ordered = inOrder(fixture.sessions, fixture.player);
            ordered.verify(fixture.sessions).remove(fixture.owner);
            ordered.verify(fixture.player).openInventory(restored);
            factory.verify(() -> UtilityMenuFactory.createTrashBox(owner, snapshot));
        }
    }

    @Test
    void doesNotSendAnOldTeleportResultToADisconnectedPlayer() {
        try (Fixture fixture = new Fixture()) {
            Location death = mock(Location.class);
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            when(death.isWorldLoaded()).thenReturn(true);
            when(fixture.deaths.find(fixture.owner)).thenReturn(Optional.of(death));
            when(fixture.player.teleportAsync(death)).thenReturn(result);

            fixture.actions.handleMenuClick(fixture.player, item(Material.ARMOR_STAND));
            when(fixture.player.isOnline()).thenReturn(false);
            result.complete(true);
            fixture.runNextTask();

            verify(fixture.player, never()).sendMessage(any(net.kyori.adventure.text.Component.class));
        }
    }

    private static ItemStack item(Material material) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.clone()).thenReturn(item);
        return item;
    }

    private static void verifyNoGameModeChanges(Player player) {
        for (GameMode mode : GameMode.values()) {
            verify(player, never()).setGameMode(Objects.requireNonNull(mode));
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final UUID owner = UUID.randomUUID();
        private final Player player = mock(Player.class);
        private final Inventory inventory = mock(Inventory.class);
        private final InventoryView view = mock(InventoryView.class);
        private final Plugin plugin = mock(Plugin.class);
        private final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        private final PlayerDeathLocations deaths = mock(PlayerDeathLocations.class);
        private final TrashInventorySessions sessions = mock(TrashInventorySessions.class);
        private final List<Runnable> tasks = new ArrayList<>();
        private final MockedStatic<? extends @Nullable Bukkit> bukkit;
        private final UtilityMenuActions actions;

        private Fixture() {
            when(plugin.isEnabled()).thenReturn(true);
            when(player.getUniqueId()).thenReturn(owner);
            when(player.isOnline()).thenReturn(true);
            when(player.getOpenInventory()).thenReturn(view);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(view.getTopInventory()).thenReturn(inventory);
            when(inventory.getHolder(false)).thenReturn(new OwnedMenuInventoryHolder(UtilityMenuKind.MENU, owner));
            ItemStack compass = item(Material.COMPASS);
            when(inventory.getItem(4)).thenReturn(compass);
            when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                tasks.add(invocation.getArgument(1));
                return mock(BukkitTask.class);
            });
            actions = new UtilityMenuActions(plugin, deaths, mock(PlayerChatListener.class), mock(PlayerNicknameService.class),
                    mock(CreeperBlockProtection.class), sessions);
            // 他の初期化が終わってから登録し、コンストラクター失敗時も static mock を残さない。
            bukkit = mockStatic(Bukkit.class);
            try {
                bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            } catch (RuntimeException | Error failure) {
                bukkit.close();
                throw failure;
            }
        }

        private void runNextTask() {
            assertFalse(tasks.isEmpty());
            tasks.removeFirst().run();
        }

        @Override
        public void close() {
            bukkit.close();
        }
    }
}
