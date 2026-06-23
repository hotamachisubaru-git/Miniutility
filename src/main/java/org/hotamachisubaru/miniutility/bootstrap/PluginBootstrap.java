package org.hotamachisubaru.miniutility.bootstrap;

import org.hotamachisubaru.miniutility.Miniutility;
import org.hotamachisubaru.miniutility.UpdateChecker;
import org.hotamachisubaru.miniutility.commands.MiniutilityCommand;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;
import org.hotamachisubaru.miniutility.death.DeathLocationStore;
import org.hotamachisubaru.miniutility.listeners.ChatListener;
import org.hotamachisubaru.miniutility.listeners.CreeperProtectionListener;
import org.hotamachisubaru.miniutility.listeners.DeathListener;
import org.hotamachisubaru.miniutility.listeners.GuiListener;
import org.hotamachisubaru.miniutility.nicknames.NicknameDatabase;
import org.hotamachisubaru.miniutility.nicknames.NicknameManager;
import org.hotamachisubaru.miniutility.nicknames.NicknameMigration;
import org.hotamachisubaru.miniutility.registry.CommandRegistry;
import org.hotamachisubaru.miniutility.registry.ListenerRegistry;
import org.hotamachisubaru.miniutility.ui.GuiActionService;
import org.hotamachisubaru.miniutility.ui.TrashBoxSessionStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class PluginBootstrap {

    private final Miniutility plugin;
    private final DeathLocationStore deathLocationStore;

    public PluginBootstrap(Miniutility plugin, DeathLocationStore deathLocationStore) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.deathLocationStore = Objects.requireNonNull(deathLocationStore, "deathLocationStore");
    }

    public PluginRuntime enable() {
        plugin.saveDefaultConfig();
        ensureDataFolder();

        NicknameDatabase nicknameDatabase = new NicknameDatabase(plugin);
        nicknameDatabase.initialize();
        new NicknameMigration(plugin, nicknameDatabase).migrateToDatabase();

        NicknameManager nicknameManager = new NicknameManager(plugin, nicknameDatabase);
        nicknameManager.reload();

        ChatListener chatListener = new ChatListener(plugin, nicknameManager);
        CreeperProtectionService creeperProtectionService = new CreeperProtectionService(true);
        CreeperProtectionListener creeperProtectionListener = new CreeperProtectionListener(creeperProtectionService);
        TrashBoxSessionStore trashBoxSessionStore = new TrashBoxSessionStore();
        GuiActionService guiActionService = new GuiActionService(
                plugin,
                deathLocationStore,
                chatListener,
                nicknameManager,
                creeperProtectionService,
                trashBoxSessionStore
        );

        registerListeners(chatListener, creeperProtectionListener, trashBoxSessionStore, guiActionService);
        registerCommands();

        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.getLogger().info("LuckPerms が見つかりません。プレフィックスなしで続行します。");
        }

        UpdateChecker updateChecker = new UpdateChecker(plugin);
        updateChecker.start();

        return new PluginRuntime(nicknameManager, chatListener, creeperProtectionService, updateChecker);
    }

    private void registerListeners(
            ChatListener chatListener,
            CreeperProtectionListener creeperProtectionListener,
            TrashBoxSessionStore trashBoxSessionStore,
            GuiActionService guiActionService
    ) {
        new ListenerRegistry(plugin).register(
                chatListener,
                creeperProtectionListener,
                new DeathListener(deathLocationStore),
                new GuiListener(guiActionService, trashBoxSessionStore)
        );
    }

    private void registerCommands() {
        new CommandRegistry(plugin).register(
                new MiniutilityCommand(plugin),
                "menu",
                "load",
                "prefixtoggle"
        );
    }

    private void ensureDataFolder() {
        Path dataFolder = plugin.getDataFolder().toPath();
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException exception) {
            throw new IllegalStateException("データフォルダの作成に失敗しました: " + dataFolder, exception);
        }
    }
}
