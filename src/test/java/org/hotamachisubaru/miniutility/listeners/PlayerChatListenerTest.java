package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.event.player.AsyncPlayerChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlayerChatListenerTest {

    private final UUID playerId = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final PlayerNicknameService manager = mock(PlayerNicknameService.class);
    private final Queue<Runnable> serverTasks = new ArrayDeque<>();
    private boolean onServerThread;
    private PlayerChatListener listener;

    @BeforeEach
    void initialize() {
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isOnline()).thenReturn(true);
        when(manager.getCachedDisplayNameComponent(playerId)).thenReturn(Optional.<Component>of(Component.text("Alice")));
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            if (onServerThread) {
                action.run();
            } else {
                serverTasks.add(action);
            }
            return null;
        }).when(manager).runOnServerThread(any(Runnable.class));
        listener = new PlayerChatListener(manager);
    }

    @Test
    void ordinaryChatUsesOnlyCachedNameAndPreservesTheMessage() {
        AsyncPlayerChatEvent event = chat("hello");
        listener.onAsyncChat(event);
        String format = assignedRenderer(event);
        assertEquals("Alice » hello", org.bukkit.ChatColor.stripColor(String.format(format, "Original", "hello")));
        verify(manager, never()).getDisplayNameComponent(any(Player.class));
        verify(event, never()).setCancelled(true);
        assertTrue(serverTasks.isEmpty());
    }

    @Test
    void validNicknameIsPrivateAndConsumedExactlyOnceOnTheServerThread() {
        listener.awaitNicknameInput(player);
        AsyncPlayerChatEvent input = chat("  ほしまち  ");
        listener.onAsyncChat(input);
        verify(input).setCancelled(true);
        verify(manager, never()).setNickname(any(), anyString());
        runServerTasks();
        verify(manager).setNickname(player, "ほしまち");
        AsyncPlayerChatEvent next = chat("ordinary");
        listener.onAsyncChat(next);
        verify(next, never()).setCancelled(true);
        assertNotNull(assignedRenderer(next));
    }

    @Test
    void invalidNicknameRetainsTheSessionUntilAValidRetry() {
        listener.awaitNicknameInput(player);
        listener.onAsyncChat(chat("invalid.name"));
        runServerTasks();
        verify(manager, never()).setNickname(any(), anyString());
        listener.onAsyncChat(chat("Valid_1"));
        runServerTasks();
        verify(manager).setNickname(player, "Valid_1");
    }

    @Test
    void aNewSessionWithTheSameModeRejectsAnAlreadyQueuedValue() {
        listener.awaitNicknameInput(player);
        listener.onAsyncChat(chat("Old"));
        listener.awaitNicknameInput(player);
        runServerTasks();
        verify(manager, never()).setNickname(any(), anyString());
        listener.onAsyncChat(chat("New"));
        runServerTasks();
        verify(manager).setNickname(player, "New");
    }

    @Test
    void twoQueuedRepliesCannotApplyOneExperienceSessionTwice() {
        when(player.getLevel()).thenReturn(5, 6);
        listener.awaitExpInput(player);
        listener.onAsyncChat(chat("1"));
        listener.onAsyncChat(chat("100"));
        runServerTasks();
        verify(player).giveExpLevels(1);
        verify(player, never()).giveExpLevels(100);
    }

    @Test
    void quitDiscardsQueuedInputAndDisplaySnapshots() {
        listener.awaitNicknameInput(player);
        listener.onAsyncChat(chat("Queued"));
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        listener.onPlayerQuit(quit);
        runServerTasks();
        verify(manager, never()).setNickname(any(), anyString());
        verify(manager).forgetPlayer(playerId);
        AsyncPlayerChatEvent ordinary = chat("hello");
        listener.onAsyncChat(ordinary);
        verify(ordinary, never()).setCancelled(true);
    }

    @Test
    void offlinePlayerCannotReceiveQueuedMutation() {
        listener.awaitExpInput(player);
        listener.onAsyncChat(chat("100"));
        when(player.isOnline()).thenReturn(false);
        runServerTasks();
        verify(player, never()).giveExpLevels(anyInt());
    }

    @NullMarked
    private AsyncPlayerChatEvent chat(String message) {
        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getMessage()).thenReturn(message);
        return event;
    }

    private void runServerTasks() {
        onServerThread = true;
        try {
            while (!serverTasks.isEmpty()) {
                serverTasks.remove().run();
            }
        } finally {
            onServerThread = false;
        }
    }

    private static String assignedRenderer(AsyncPlayerChatEvent event) {
        var rendererCalls = mockingDetails(event).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("setFormat")
                        && invocation.getArguments().length == 1)
                .toList();
        assertEquals(1, rendererCalls.size());
        return (String) rendererCalls.get(0).getArguments()[0];
    }
}
