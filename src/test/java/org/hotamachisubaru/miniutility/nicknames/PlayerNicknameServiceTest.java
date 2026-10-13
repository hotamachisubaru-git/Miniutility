package org.hotamachisubaru.miniutility.nicknames;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.hotamachisubaru.miniutility.MiniutilityPlugin;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.hotamachisubaru.miniutility.util.LuckPermsPrefixProvider;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class PlayerNicknameServiceTest {

    @Test
    void failedSaveCannotPublishOrKeepAnUnpersistedNickname() {
        try (Fixture fixture = new Fixture()) {
            doThrow(new IllegalStateException("database failure")).when(fixture.database)
                    .saveNickname(fixture.owner, "Unpersisted");

            assertThrows(IllegalStateException.class, () -> fixture.manager.setNickname(fixture.player, "Unpersisted"));

            assertEquals("Original", fixture.manager.getDisplayName(fixture.player));
            assertTrue(fixture.manager.getCachedDisplayNameComponent(fixture.owner).isEmpty());
            verify(fixture.player, never()).displayName(any(Component.class));
        }
    }

    @Test
    void failedDeletionKeepsThePersistedNicknameAndDisplaySnapshot() {
        try (Fixture fixture = new Fixture()) {
            fixture.manager.setNickname(fixture.player, "Stored");
            doThrow(new IllegalStateException("database failure")).when(fixture.database).deleteNickname(fixture.owner);

            assertThrows(IllegalStateException.class, () -> fixture.manager.removeNickname(fixture.player));

            assertEquals("Stored", fixture.manager.getDisplayName(fixture.player));
            assertEquals("Stored", LegacyTextFormatter.plain(Objects.requireNonNull(
                    fixture.manager.getCachedDisplayNameComponent(fixture.owner).orElseThrow())));
        }
    }

    @Test
    void cachedChatReadUsesNoBukkitCallsAndReflectsLiveCachedPrefixChanges() {
        try (Fixture fixture = new Fixture()) {
            fixture.manager.setNickname(fixture.player, "Alice");
            clearInvocations(fixture.player, fixture.plugin);

            fixture.prefix.set("&6[VIP]");
            Component first = fixture.manager.getCachedDisplayNameComponent(fixture.owner).orElseThrow();
            fixture.prefix.set("&b[Member]");
            Component second = fixture.manager.getCachedDisplayNameComponent(fixture.owner).orElseThrow();

            assertEquals("[VIP] Alice", LegacyTextFormatter.plain(Objects.requireNonNull(first)));
            assertEquals("[Member] Alice", LegacyTextFormatter.plain(Objects.requireNonNull(second)));
            verifyNoInteractions(fixture.player, fixture.plugin);
            fixture.manager.forgetPlayer(fixture.owner);
            assertTrue(fixture.manager.getCachedDisplayNameComponent(fixture.owner).isEmpty());
        }
    }

    @Test
    void queuedTaskCannotMutateStateAfterThePluginIsDisabled() {
        try (Fixture fixture = new Fixture()) {
            Runnable action = mock(Runnable.class);
            fixture.manager.runOnServerThread(action);
            assertEquals(1, fixture.tasks.size());
            fixture.enabled.set(false);

            fixture.tasks.removeFirst().run();
            fixture.manager.runOnServerThread(action);

            verifyNoInteractions(action);
            assertTrue(fixture.tasks.isEmpty());
        }
    }

    @Test
    void schedulerDisableRaceIsIgnoredForChatAndLuckPermsRefreshCallbacks() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.scheduler.runTask(eq(fixture.plugin), any(Runnable.class))).thenAnswer(invocation -> {
                fixture.enabled.set(false);
                throw new IllegalPluginAccessException("disabled during enqueue");
            });

            assertDoesNotThrow(() -> fixture.manager.runOnServerThread(() -> fail("must not execute")));
            fixture.enabled.set(true);
            assertDoesNotThrow(() -> fixture.refresh.get().accept(fixture.owner));
            assertTrue(fixture.tasks.isEmpty());
        }
    }

    @Test
    void schedulerFailureWhileEnabledIsNotSilentlySuppressed() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.scheduler.runTask(eq(fixture.plugin), any(Runnable.class)))
                    .thenThrow(new IllegalPluginAccessException("unexpected failure"));

            assertThrows(IllegalPluginAccessException.class, () -> fixture.manager.runOnServerThread(() -> {}));
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final UUID owner = UUID.randomUUID();
        private final MiniutilityPlugin plugin = mock(MiniutilityPlugin.class);
        private final Player player = mock(Player.class);
        private final Server server = mock(Server.class);
        private final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        private final SqliteNicknameRepository database = mock(SqliteNicknameRepository.class);
        private final List<Runnable> tasks = new ArrayList<>();
        private final AtomicBoolean enabled = new AtomicBoolean(true);
        private final AtomicReference<String> prefix = new AtomicReference<>("");
        private final AtomicReference<Consumer<UUID>> refresh = new AtomicReference<>();
        private final MockedStatic<? extends @Nullable Bukkit> bukkit = mockStatic(Bukkit.class);
        private final MockedStatic<? extends @Nullable LuckPermsPrefixProvider> luckPerms = mockStatic(LuckPermsPrefixProvider.class);
        private final PlayerNicknameService manager;

        private Fixture() {
            when(plugin.getServer()).thenReturn(server);
            when(plugin.getLogger()).thenReturn(Logger.getLogger("PlayerNicknameServiceTest"));
            when(plugin.getConfig()).thenReturn(new YamlConfiguration());
            when(plugin.isEnabled()).thenAnswer(invocation -> enabled.get());
            when(server.getScheduler()).thenReturn(scheduler);
            when(player.getUniqueId()).thenReturn(owner);
            when(player.getName()).thenReturn("Original");
            Supplier<String> prefixLookup = prefix::get;
            luckPerms.when(() -> LuckPermsPrefixProvider.capturePrefixLookup(player)).thenReturn(prefixLookup);
            luckPerms.when(() -> LuckPermsPrefixProvider.registerRefreshListeners(eq(plugin), any())).thenAnswer(invocation -> {
                refresh.set(invocation.getArgument(1));
                return null;
            });
            when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                tasks.add(invocation.getArgument(1));
                return mock(BukkitTask.class);
            });
            manager = new PlayerNicknameService(plugin, database);
        }

        @Override
        public void close() {
            luckPerms.close();
            bukkit.close();
        }
    }
}
