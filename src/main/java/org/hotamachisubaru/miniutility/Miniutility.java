package org.hotamachisubaru.miniutility;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.hotamachisubaru.miniutility.Command.CommandManager;
import org.hotamachisubaru.miniutility.Listener.Chat;
import org.hotamachisubaru.miniutility.Listener.ChatPaperListener;
import org.hotamachisubaru.miniutility.Listener.CreeperProtectionListener;
import org.hotamachisubaru.miniutility.Listener.DeathListener;
import org.hotamachisubaru.miniutility.Listener.Menu;
import org.hotamachisubaru.miniutility.Listener.NicknameListener;
import org.hotamachisubaru.miniutility.Listener.TrashListener;
import org.hotamachisubaru.miniutility.Nickname.NicknameDatabase;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.Nickname.NicknameMigration;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.bukkit.Bukkit.getOnlinePlayers;

/**
 * Miniutility の本体。
 * 依存関係の生成、初期化、登録、終了処理をここで管理する。
 */
public final class Miniutility {

    private static final String UPDATE_OWNER = "minamikana-git";
    private static final String UPDATE_REPO = "Miniutility";
    private static final long INITIAL_UPDATE_DELAY_TICKS = 20L;
    private static final long DAILY_CHECK_TICKS = 20L * 60L * 60L * 24L;
    private static final int HTTP_TIMEOUT_MS = 7000;

    private final MiniutilityLoader plugin;
    private final Logger logger;
    private final PluginManager pluginManager;
    private final HttpClient httpClient;

    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    private final NicknameDatabase nicknameDatabase;
    private final NicknameManager nicknameManager;
    private final CreeperProtectionListener creeperProtectionListener;
    private final Chat chatListener;
    private final TrashListener trashListener;
    private final NicknameListener nicknameListener;
    private final Menu menuListener;
    private final ChatPaperListener chatPaperListener;
    private final DeathListener deathListener;

    private volatile String lastNotifiedVersion;
    private volatile boolean shuttingDown;

    public Miniutility(MiniutilityLoader plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.pluginManager = plugin.getServer().getPluginManager();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(HTTP_TIMEOUT_MS))
                .build();

        this.nicknameDatabase = new NicknameDatabase(plugin);
        this.nicknameManager = new NicknameManager(nicknameDatabase, logger);
        this.creeperProtectionListener = new CreeperProtectionListener();
        this.chatListener = new Chat(plugin, nicknameManager);
        this.trashListener = new TrashListener(plugin);
        this.nicknameListener = new NicknameListener(chatListener, nicknameManager);
        this.menuListener = new Menu(plugin, this, chatListener, trashListener);
        this.chatPaperListener = new ChatPaperListener(chatListener, nicknameManager);
        this.deathListener = new DeathListener(this);
    }

    public void enable() {
        plugin.saveDefaultConfig();
        nicknameDatabase.init();
        migrateNicknamesIfNeeded();
        nicknameManager.reload(getOnlinePlayers());

        registerListeners();
        registerCommands();
        checkLuckPerms();

        checkForUpdates();
        scheduleDailyUpdateCheck();

        logger.info("copyright 2024-2025 hotamachisubaru all rights reserved.");
        logger.info("developed by hotamachisubaru");
    }

    public void disable() {
        shuttingDown = true;
        nicknameDatabase.saveAll(nicknameManager.snapshotNicknames());
    }

    private void migrateNicknamesIfNeeded() {
        new NicknameMigration(plugin, nicknameDatabase).migrateToDatabase();
    }

    private void registerListeners() {
        pluginManager.registerEvents(chatListener, plugin);
        pluginManager.registerEvents(chatPaperListener, plugin);
        pluginManager.registerEvents(creeperProtectionListener, plugin);
        pluginManager.registerEvents(deathListener, plugin);
        pluginManager.registerEvents(menuListener, plugin);
        pluginManager.registerEvents(nicknameListener, plugin);
        pluginManager.registerEvents(trashListener, plugin);
    }

    private void registerCommands() {
        CommandManager commandManager = new CommandManager(this);
        registerCommand("menu", commandManager);
        registerCommand("load", commandManager);
        registerCommand("prefixtoggle", commandManager);
    }

    private void registerCommand(String commandName, CommandManager manager) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command == null) {
            logger.severe("plugin.yml にコマンド " + commandName + " が定義されていません。");
            return;
        }
        command.setExecutor(manager);
        command.setTabCompleter(manager);
    }

    private void checkLuckPerms() {
        if (pluginManager.getPlugin("LuckPerms") == null) {
            logger.info("LuckPerms が見つからないため、Prefix は空文字として扱います。");
        }
    }

    private void scheduleDailyUpdateCheck() {
        FoliaUtil.runLater(plugin, this::checkForUpdatesAndReschedule, INITIAL_UPDATE_DELAY_TICKS);
    }

    private void checkForUpdatesAndReschedule() {
        if (shuttingDown || !plugin.isEnabled()) {
            return;
        }

        try {
            checkForUpdates();
        } catch (Throwable t) {
            logger.warning("アップデートのチェックに失敗しました: " + t.getMessage());
        }

        FoliaUtil.runLater(plugin, this::checkForUpdatesAndReschedule, DAILY_CHECK_TICKS);
    }

    private void checkForUpdates() {
        String apiUrl = "https://api.github.com/repos/" + UPDATE_OWNER + "/" + UPDATE_REPO + "/releases/latest";
        CompletableFuture
                .supplyAsync(() -> requestLatestRelease(apiUrl))
                .thenAccept(this::handleLatestReleaseResponse);
    }

    private HttpResp requestLatestRelease(String apiUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(HTTP_TIMEOUT_MS))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Miniutility/" + getPluginVersion())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResp(response.statusCode(), response.body());
        } catch (Exception e) {
            logger.warning("アップデートチェックの通信に失敗しました: " + e.getMessage());
            return HttpResp.failed();
        }
    }

    private void handleLatestReleaseResponse(HttpResp response) {
        if (response.code != 200 || response.body == null || response.body.isBlank()) {
            if (response.code != -1) {
                logger.warning("アップデート情報の取得に失敗しました: HTTP " + response.code);
            }
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(response.body).getAsJsonObject();
            String latestTag = normalizeVersion(readString(json, "tag_name"));
            String releaseUrl = readString(json, "html_url");
            String currentVersion = normalizeVersion(getPluginVersion());

            if (latestTag.isBlank() || latestTag.equals(currentVersion) || latestTag.equals(lastNotifiedVersion)) {
                return;
            }

            lastNotifiedVersion = latestTag;
            String message = "新しいバージョン " + latestTag + " が利用可能です: " + releaseUrl;
            logger.info(message);

            for (Player player : getOnlinePlayers()) {
                if (!player.isOp()) {
                    continue;
                }
                FoliaUtil.runAtPlayer(plugin, player.getUniqueId(), () -> player.sendMessage(message));
            }
        } catch (Exception e) {
            logger.warning("アップデート情報の解析に失敗しました: " + e.getMessage());
        }
    }

    private static String readString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        return version.replaceFirst("^v", "").trim();
    }

    private String getPluginVersion() {
        try {
            return plugin.getDescription().getVersion();
        } catch (Throwable ignored) {
            return "";
        }
    }

    public void setDeathLocation(UUID uuid, Location location) {
        if (uuid == null || location == null) {
            return;
        }
        deathLocations.put(uuid, location.clone());
    }

    public Location getDeathLocation(UUID uuid) {
        Location location = deathLocations.get(uuid);
        return location == null ? null : location.clone();
    }

    public MiniutilityLoader getPlugin() {
        return plugin;
    }

    public NicknameDatabase getNicknameDatabase() {
        return nicknameDatabase;
    }

    public NicknameManager getNicknameManager() {
        return nicknameManager;
    }

    public CreeperProtectionListener getCreeperProtectionListener() {
        return creeperProtectionListener;
    }

    public Chat getChatListener() {
        return chatListener;
    }

    private static final class HttpResp {
        private final int code;
        private final String body;

        private HttpResp(int code, String body) {
            this.code = code;
            this.body = body;
        }

        private static HttpResp failed() {
            return new HttpResp(-1, null);
        }
    }
}

