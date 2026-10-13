package org.hotamachisubaru.miniutility.listeners;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
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
        AsyncChatEvent event = chat("hello");
        listener.onAsyncChat(event);
        ChatRenderer renderer = assignedRenderer(event);
        assertNotNull(renderer);
        Player source = Objects.requireNonNull(player);
        assertEquals("Alice » hello", LegacyTextFormatter.plain(renderer.render(
                source, Component.text("Original"), event.message(), source)));
        verify(manager, never()).getDisplayNameComponent(any(Player.class));
        verify(event, never()).setCancelled(true);
        assertTrue(serverTasks.isEmpty());
    }

    @Test
    void validNicknameIsPrivateAndConsumedExactlyOnceOnTheServerThread() {
        listener.awaitNicknameInput(player);
        AsyncChatEvent input = chat("  ほしまち  ");
        listener.onAsyncChat(input);
        verify(input).setCancelled(true);
        verify(manager, never()).setNickname(any(), anyString());
        runServerTasks();
        verify(manager).setNickname(player, "ほしまち");
        AsyncChatEvent next = chat("ordinary");
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
        AsyncChatEvent ordinary = chat("hello");
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
    private AsyncChatEvent chat(String message) {
        AsyncChatEvent event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.message()).thenReturn(Component.text(message));
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

    private static ChatRenderer assignedRenderer(AsyncChatEvent event) {
        var rendererCalls = mockingDetails(event).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("renderer")
                        && invocation.getArguments().length == 1)
                .toList();
        assertEquals(1, rendererCalls.size());
        return (ChatRenderer) rendererCalls.getFirst().getArguments()[0];
    }
}
