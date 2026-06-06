package org.hotamachisubaru.miniutility.bootstrap;

import org.hotamachisubaru.miniutility.Listener.Chat;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.UpdateChecker;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;

import java.util.Objects;

public final class PluginRuntime {

    private final NicknameManager nicknameManager;
    private final Chat chatListener;
    private final CreeperProtectionService creeperProtectionService;
    private final UpdateChecker updateChecker;

    public PluginRuntime(
            NicknameManager nicknameManager,
            Chat chatListener,
            CreeperProtectionService creeperProtectionService,
            UpdateChecker updateChecker
    ) {
        this.nicknameManager = Objects.requireNonNull(nicknameManager);
        this.chatListener = Objects.requireNonNull(chatListener);
        this.creeperProtectionService = Objects.requireNonNull(creeperProtectionService);
        this.updateChecker = Objects.requireNonNull(updateChecker);
    }

    public void shutdown() {
        updateChecker.stop();
        nicknameManager.persistAll();
    }

    public NicknameManager getNicknameManager() {
        return nicknameManager;
    }

    public Chat getChatListener() {
        return chatListener;
    }

    public CreeperProtectionService getCreeperProtectionService() {
        return creeperProtectionService;
    }
}
