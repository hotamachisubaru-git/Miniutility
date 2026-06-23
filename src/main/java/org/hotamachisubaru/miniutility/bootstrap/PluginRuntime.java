package org.hotamachisubaru.miniutility.bootstrap;

import org.hotamachisubaru.miniutility.UpdateChecker;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;
import org.hotamachisubaru.miniutility.listeners.ChatListener;
import org.hotamachisubaru.miniutility.nicknames.NicknameManager;

import java.util.Objects;

public final class PluginRuntime {

    private final NicknameManager nicknameManager;
    private final ChatListener chatListener;
    private final CreeperProtectionService creeperProtectionService;
    private final UpdateChecker updateChecker;

    public PluginRuntime(
            NicknameManager nicknameManager,
            ChatListener chatListener,
            CreeperProtectionService creeperProtectionService,
            UpdateChecker updateChecker
    ) {
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
        this.chatListener = Objects.requireNonNull(chatListener, "chatListener");
        this.creeperProtectionService = Objects.requireNonNull(
                creeperProtectionService,
                "creeperProtectionService"
        );
        this.updateChecker = Objects.requireNonNull(updateChecker, "updateChecker");
    }

    public void shutdown() {
        updateChecker.stop();
        nicknameManager.persistAll();
    }

    public NicknameManager getNicknameManager() {
        return nicknameManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }

    public CreeperProtectionService getCreeperProtectionService() {
        return creeperProtectionService;
    }
}
