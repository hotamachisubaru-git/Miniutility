package org.hotamachisubaru.miniutility.listeners.input;

import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatInputRouterTest {

    @Test
    void validationAndPlayerMutationWaitForTheServerExecutor() {
        Player player = mock(Player.class);
        PlayerNicknameService manager = mock(PlayerNicknameService.class);
        List<Runnable> tasks = new ArrayList<>();
        ChatInputRouter router = new ChatInputRouter(manager, tasks::add, (target, message) -> fail("valid input"));
        router.handle(player, ChatInputMode.EXPERIENCE, "-99");
        verifyNoInteractions(player, manager);
        when(player.getLevel()).thenReturn(8, 0);
        tasks.get(0).run();
        verify(player).giveExpLevels(-8);
        verify(player).sendMessage(anyString());
    }

    @Test
    void positiveExperienceChangeCannotOverflowTheCurrentLevel() {
        Player player = mock(Player.class);
        when(player.getLevel()).thenReturn(Integer.MAX_VALUE - 3);
        var router = new ChatInputRouter(mock(PlayerNicknameService.class), action -> Objects.requireNonNull(action).run(),
                (target, message) -> fail());
        router.handle(player, ChatInputMode.EXPERIENCE, Objects.requireNonNull(Integer.toString(Integer.MAX_VALUE)));
        verify(player).giveExpLevels(3);
    }

    @Test
    void invalidExperienceKeepsSessionOwnershipAndNeverChangesThePlayer() {
        Player player = mock(Player.class);
        List<ChatInputRouter.InputMessage> errors = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        var router = new ChatInputRouter(mock(PlayerNicknameService.class), action -> Objects.requireNonNull(action).run(),
                (target, message) -> errors.add(message));
        router.handle(player, ChatInputMode.EXPERIENCE, "2147483648", action -> completions.incrementAndGet());
        assertEquals(0, completions.get());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).text().contains("数値"));
        verifyNoInteractions(player);
    }

    @Test
    void onlyTheSessionOwnerCanApplyAValidNickname() {
        Player player = mock(Player.class);
        PlayerNicknameService manager = mock(PlayerNicknameService.class);
        List<Runnable> completions = new ArrayList<>();
        var router = new ChatInputRouter(manager, action -> Objects.requireNonNull(action).run(),
                (target, message) -> fail());
        router.handle(player, ChatInputMode.COLORED_NICKNAME, "&bほしまち", completions::add);
        verifyNoInteractions(manager, player);
        completions.get(0).run();
        verify(manager).setNickname(player, LegacyTextFormatter.ampersandToSection("&bほしまち"));
    }
}
