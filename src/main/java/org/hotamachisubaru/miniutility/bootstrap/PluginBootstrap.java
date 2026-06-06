package org.hotamachisubaru.miniutility.bootstrap;

import org.hotamachisubaru.miniutility.Command.CommandManager;
import org.hotamachisubaru.miniutility.GUI.GuiActionService;
import org.hotamachisubaru.miniutility.GUI.TrashBoxSessionStore;
import org.hotamachisubaru.miniutility.Listener.Chat;
import org.hotamachisubaru.miniutility.Listener.CreeperProtectionListener;
import org.hotamachisubaru.miniutility.Listener.DeathListener;
import org.hotamachisubaru.miniutility.Listener.GuiListener;
import org.hotamachisubaru.miniutility.Miniutility;
import org.hotamachisubaru.miniutility.Nickname.NicknameDatabase;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.Nickname.NicknameMigration;
import org.hotamachisubaru.miniutility.UpdateChecker;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;
import org.hotamachisubaru.miniutility.death.DeathLocationStore;
import org.hotamachisubaru.miniutility.registry.CommandRegistry;
import org.hotamachisubaru.miniutility.registry.ListenerRegistry;

import java.io.File;

public final class PluginBootstrap {

    private final Miniutility plugin;
    private final DeathLocationStore deathLocationStore;

    public PluginBootstrap(Miniutility plugin, DeathLocationStore deathLocationStore) {
        this.plugin = plugin;
        this.deathLocationStore = deathLocationStore;
    }

    public PluginRuntime enable() {
        plugin.saveDefaultConfig();
        ensureDataFolder();

        NicknameDatabase nicknameDatabase = new NicknameDatabase(plugin);
        nicknameDatabase.initialize();
        new NicknameMigration(plugin, nicknameDatabase).migrateToDatabase();

        NicknameManager nicknameManager = new NicknameManager(plugin, nicknameDatabase);
        nicknameManager.reload();

        Chat chatListener = new Chat(plugin, nicknameManager);
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
            plugin.getLogger().info("LuckPermsが見つかりません。プレフィックスなしで続行します。");
        }

        UpdateChecker updateChecker = new UpdateChecker(plugin);
        updateChecker.start();

        return new PluginRuntime(nicknameManager, chatListener, creeperProtectionService, updateChecker);
    }

    private void registerListeners(
            Chat chatListener,
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
        new CommandRegistry(plugin).register(new CommandManager(plugin), "menu", "load", "prefixtoggle");
    }

    private void ensureDataFolder() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("データフォルダの作成に失敗しました: " + dataFolder.getAbsolutePath());
        }
    }
}
