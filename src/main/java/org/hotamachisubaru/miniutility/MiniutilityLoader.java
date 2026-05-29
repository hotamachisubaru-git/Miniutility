package org.hotamachisubaru.miniutility;

import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.hotamachisubaru.miniutility.Command.CommandManager;
import org.hotamachisubaru.miniutility.Listener.Chat;
import org.hotamachisubaru.miniutility.Listener.CreeperProtectionListener;
import org.hotamachisubaru.miniutility.Listener.DeathListener;
import org.hotamachisubaru.miniutility.Listener.GuiListener;
import org.hotamachisubaru.miniutility.Nickname.NicknameDatabase;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.Nickname.NicknameMigration;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MiniutilityLoader extends JavaPlugin {

    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();
    private NicknameDatabase nicknameDatabase;
    private NicknameManager nicknameManager;
    private Chat chatListener;
    private CreeperProtectionListener creeperProtectionListener;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureDataFolder();

        this.nicknameDatabase = new NicknameDatabase(this);
        nicknameDatabase.initialize();
        new NicknameMigration(this, nicknameDatabase).migrateToDatabase();

        this.nicknameManager = new NicknameManager(this, nicknameDatabase);
        nicknameManager.reload();

        this.chatListener = new Chat(this, nicknameManager);
        this.creeperProtectionListener = new CreeperProtectionListener();

        registerListeners();
        registerCommands();
        nicknameManager.refreshOnlinePlayers();

        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            getLogger().info("LuckPermsが見つかりません。Prefixなしで続行します。");
        }

        this.updateChecker = new UpdateChecker(this);
        updateChecker.start();

        getLogger().info("copyright 2024-2026 hotamachisubaru all rights reserved.");
        getLogger().info("developed by hotamachisubaru");
    }

    @Override
    public void onDisable() {
        if (updateChecker != null) {
            updateChecker.stop();
        }
        if (nicknameManager != null) {
            nicknameManager.persistAll();
        }
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(chatListener, this);
        pluginManager.registerEvents(creeperProtectionListener, this);
        pluginManager.registerEvents(new DeathListener(this), this);
        pluginManager.registerEvents(new GuiListener(this), this);
    }

    private void registerCommands() {
        CommandManager commandManager = new CommandManager(this);
        registerCommand("menu", commandManager);
        registerCommand("load", commandManager);
        registerCommand("prefixtoggle", commandManager);
    }

    private void registerCommand(String commandName, CommandManager commandManager) {
        PluginCommand command = getCommand(Objects.requireNonNull(commandName));
        if (command == null) {
            throw new IllegalStateException("plugin.yml にコマンド " + commandName + " が定義されていません。");
        }

        command.setExecutor(commandManager);
        command.setTabCompleter(commandManager);
    }

    private void ensureDataFolder() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("データフォルダの作成に失敗しました: " + dataFolder.getAbsolutePath());
        }
    }

    public void recordDeathLocation(UUID uniqueId, Location location) {
        if (uniqueId == null || location == null) {
            return;
        }
        deathLocations.put(uniqueId, location.clone());
    }

    public Location getDeathLocation(UUID uniqueId) {
        Location location = deathLocations.get(uniqueId);
        return location == null ? null : location.clone();
    }

    public NicknameManager getNicknameManager() {
        return nicknameManager;
    }

    public Chat getChatListener() {
        return chatListener;
    }

    public CreeperProtectionListener getCreeperProtectionListener() {
        return creeperProtectionListener;
    }
}
