package org.hotamachisubaru.miniutility.bootstrap;

import org.hotamachisubaru.miniutility.ReleaseUpdateNotifier;
import org.hotamachisubaru.miniutility.listeners.PlayerChatListener;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MiniutilityServicesTest {
    @Test
    void closesOnlyOnce() {
        var manager = mock(PlayerNicknameService.class);
        var checker = mock(ReleaseUpdateNotifier.class);
        var runtime = new MiniutilityServices(manager, mock(PlayerChatListener.class), checker);
        runtime.close();
        runtime.close();
        verify(checker).stop();
        verify(manager).persistAll();
    }

    @Test
    void attemptsPersistenceAndRetainsBothCleanupFailures() {
        var manager = mock(PlayerNicknameService.class);
        var checker = mock(ReleaseUpdateNotifier.class);
        var first = new IllegalStateException("stop");
        var second = new IllegalStateException("persist");
        doThrow(first).<ReleaseUpdateNotifier>when(checker).stop();
        doThrow(second).<PlayerNicknameService>when(manager).persistAll();
        var runtime = new MiniutilityServices(manager, mock(PlayerChatListener.class), checker);
        assertSame(first, assertThrows(IllegalStateException.class, runtime::close));
        assertArrayEquals(new Throwable[]{second}, first.getSuppressed());
    }
}
