package org.hotamachisubaru.miniutility.bootstrap;

import org.hotamachisubaru.miniutility.MiniutilityPlugin;
import org.hotamachisubaru.miniutility.ReleaseUpdateNotifier;
import org.hotamachisubaru.miniutility.commands.UtilityCommandHandlers;
import org.hotamachisubaru.miniutility.creeper.CreeperBlockProtection;
import org.hotamachisubaru.miniutility.death.PlayerDeathLocations;
import org.hotamachisubaru.miniutility.listeners.PlayerChatListener;
import org.hotamachisubaru.miniutility.listeners.CreeperExplosionListener;
import org.hotamachisubaru.miniutility.listeners.PlayerDeathListener;
import org.hotamachisubaru.miniutility.listeners.UtilityMenuListener;
import org.hotamachisubaru.miniutility.nicknames.SqliteNicknameRepository;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.nicknames.LegacyNicknameImporter;
import org.hotamachisubaru.miniutility.registry.BukkitCommandRegistrar;
import org.hotamachisubaru.miniutility.registry.PluginListenerRegistrar;
import org.hotamachisubaru.miniutility.ui.UtilityMenuActions;
import org.hotamachisubaru.miniutility.ui.TrashInventorySessions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * MiniutilityPlugin プラグインの起動処理を担うクラス。
 * <p>
 * 初期化の各ステップをメソッドに分割し、{@link MiniutilityServices} を生成して返します。
 */
public final class MiniutilityInitializer {

    private static final String LUCK_PERMS_PLUGIN_NAME = "LuckPerms";

    private final MiniutilityPlugin plugin;
    private final PlayerDeathLocations deathLocationStore;

    public MiniutilityInitializer(MiniutilityPlugin plugin, PlayerDeathLocations deathLocationStore) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.deathLocationStore = Objects.requireNonNull(deathLocationStore, "deathLocationStore");
    }

    // ─── 起動処理 ──────────────────────────────────────────────

    /**
     * プラグインの起動処理を実行し、ランタイムを返します。
     */
    public MiniutilityServices enable() {
        initializeEnvironment();
        PlayerNicknameService nicknameManager = initializeNicknameSystem();
        ReleaseUpdateNotifier updateChecker = new ReleaseUpdateNotifier(plugin);
        try {
            BootstrapComponents components = initializeCoreComponents(nicknameManager);
            registerAll(components);
            checkOptionalDependencies();
            updateChecker.start();
            return new MiniutilityServices(nicknameManager, components.chatListener(), updateChecker,
                    components.trashBoxSessionStore());
        } catch (RuntimeException exception) {
            updateChecker.stop();
            try {
                nicknameManager.persistAll();
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    // ─── 個別初期化メソッド ────────────────────────────────────

    /**
     * 設定ファイルのデフォルト展開とデータフォルダの作成を行います。
     */
    private void initializeEnvironment() {
        plugin.saveDefaultConfig();
        ensureDataFolder();
    }

    /**
     * ニックネームDBの初期化・マイグレーション・マネージャーの構築を行います。
     */
    private PlayerNicknameService initializeNicknameSystem() {
        SqliteNicknameRepository nicknameDatabase = new SqliteNicknameRepository(plugin);
        nicknameDatabase.initialize();
        new LegacyNicknameImporter(plugin, nicknameDatabase).migrateToDatabase();

        PlayerNicknameService nicknameManager = new PlayerNicknameService(plugin, nicknameDatabase);
        nicknameManager.reload();
        return nicknameManager;
    }

    /**
     * チャットリスナー、クリーパー保護、ゴミ箱セッション、GUIアクションサービスを構築します。
     */
    private BootstrapComponents initializeCoreComponents(PlayerNicknameService nicknameManager) {
        PlayerChatListener chatListener = new PlayerChatListener(nicknameManager);
        CreeperBlockProtection creeperProtectionService = new CreeperBlockProtection(true);
        CreeperExplosionListener creeperProtectionListener = new CreeperExplosionListener(creeperProtectionService);
        TrashInventorySessions trashBoxSessionStore = new TrashInventorySessions();
        UtilityMenuActions guiActionService = new UtilityMenuActions(
                plugin,
                deathLocationStore,
                chatListener,
                nicknameManager,
                creeperProtectionService,
                trashBoxSessionStore
        );

        return new BootstrapComponents(
                chatListener,
                creeperProtectionListener,
                trashBoxSessionStore,
                guiActionService
        );
    }

    // ─── 登録処理 ──────────────────────────────────────────────

    /**
     * 全リスナーとコマンドを登録します。
     */
    private void registerAll(BootstrapComponents components) {
        registerListeners(components);
        registerCommands();
    }

    private void registerListeners(BootstrapComponents components) {
        new PluginListenerRegistrar(plugin).register(
                components.chatListener(),
                components.creeperProtectionListener(),
                new PlayerDeathListener(deathLocationStore),
                new UtilityMenuListener(components.guiActionService(), components.trashBoxSessionStore())
        );
    }

    private void registerCommands() {
        new BukkitCommandRegistrar(plugin).register(
                new UtilityCommandHandlers(plugin)::commandAdapter,
                "menu",
                "load",
                "prefixtoggle"
        );
    }

    // ─── 依存チェック ──────────────────────────────────────────

    /**
     * LuckPerms の存在を確認し、なければ警告ログを出力します。
     */
    private void checkOptionalDependencies() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled(LUCK_PERMS_PLUGIN_NAME)) {
            plugin.getLogger().info("LuckPerms が見つかりません。プレフィックスなしで続行します。");
        }
    }

    // ─── ユーティリティ ────────────────────────────────────────

    /**
     * プラグインのデータフォルダを再帰的に作成します。
     */
    private void ensureDataFolder() {
        Path dataFolder = plugin.getDataFolder().toPath();
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException exception) {
            throw new IllegalStateException("データフォルダの作成に失敗しました: " + dataFolder, exception);
        }
    }

    // ─── 内部データクラス ──────────────────────────────────────

    /**
     * 起動処理中に生成された一時的なコンポーネントをまとめた不変クラス。
     */
    private record BootstrapComponents(
            PlayerChatListener chatListener,
            CreeperExplosionListener creeperProtectionListener,
            TrashInventorySessions trashBoxSessionStore,
            UtilityMenuActions guiActionService
    ) {
    }
}
