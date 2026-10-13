package org.hotamachisubaru.miniutility.nicknames;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/** Short-lived SQLite connections with atomic batch writes and the existing nickname schema. */
public final class SqliteNicknameRepository {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS nicknames (
                uuid TEXT PRIMARY KEY,
                nickname TEXT NOT NULL
            )
            """;
    private static final String UPSERT = """
            INSERT INTO nicknames (uuid, nickname) VALUES (?, ?)
            ON CONFLICT(uuid) DO UPDATE SET nickname = excluded.nickname
            """;
    private static final String DELETE = "DELETE FROM nicknames WHERE uuid = ?";
    private static final String SELECT_ALL = "SELECT uuid, nickname FROM nicknames";

    private final Logger logger;
    private final Path databasePath;
    private final String databaseUrl;
    private final boolean autoCreate;
    private volatile boolean initialized;

    public SqliteNicknameRepository(JavaPlugin plugin) {
        this(resolveDatabasePath(Objects.requireNonNull(plugin, "plugin")),
                plugin.getConfig().getBoolean("database.autoCreate", true), plugin.getLogger());
    }

    /** Shared by the plugin and real SQLite integration tests without requiring a live server. */
    SqliteNicknameRepository(Path databasePath, boolean autoCreate, Logger logger) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath().normalize();
        this.databaseUrl = "jdbc:sqlite:" + this.databasePath;
        this.autoCreate = autoCreate;
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        if (!autoCreate && Files.notExists(databasePath)) {
            throw new IllegalStateException("ニックネームDBが存在せず、database.autoCreate が無効です。");
        }
        try {
            Files.createDirectories(databasePath.getParent());
            try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE_TABLE);
            }
            initialized = true;
        } catch (IOException | SQLException exception) {
            throw databaseFailure("初期化", exception);
        }
    }

    public void saveNickname(UUID playerId, String nickname) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(nickname, "nickname");
        ensureInitialized();
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            bindNickname(statement, playerId, nickname);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseFailure("保存", exception);
        }
    }

    public void deleteNickname(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        ensureInitialized();
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseFailure("削除", exception);
        }
    }

    public Map<UUID, String> loadAll() {
        ensureInitialized();
        var nicknames = new HashMap<UUID, String>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                addValidRow(nicknames, rows);
            }
        } catch (SQLException exception) {
            throw databaseFailure("読み込み", exception);
        }
        return Map.copyOf(nicknames);
    }

    public void saveAll(Map<UUID, String> nicknames) {
        // Freeze and validate every row before opening a transaction or changing the database.
        Map<UUID, String> snapshot = Map.copyOf(Objects.requireNonNull(nicknames, "nicknames"));
        ensureInitialized();
        if (snapshot.isEmpty()) {
            return;
        }
        try (Connection connection = openConnection()) {
            saveBatch(connection, snapshot);
        } catch (SQLException exception) {
            throw databaseFailure("一括保存", exception);
        }
    }

    private void saveBatch(Connection connection, Map<UUID, String> nicknames) throws SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            for (var entry : nicknames.entrySet()) {
                bindNickname(statement, entry.getKey(), entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        }
    }

    private void addValidRow(Map<UUID, String> nicknames, ResultSet rows) throws SQLException {
        String serializedPlayerId = rows.getString("uuid");
        String nickname = rows.getString("nickname");
        if (serializedPlayerId == null || nickname == null) {
            logger.warning("UUIDまたはニックネームが欠落したDB行をスキップしました。");
            return;
        }
        try {
            nicknames.put(UUID.fromString(serializedPlayerId), nickname);
        } catch (IllegalArgumentException exception) {
            logger.warning("不正なUUIDのニックネームデータをスキップしました: " + serializedPlayerId);
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    private static Path resolveDatabasePath(JavaPlugin plugin) {
        Path dataDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        String configuredPath = Objects.requireNonNullElse(
                plugin.getConfig().getString("database.path", "nickname.db"), "nickname.db"
        );
        Path databasePath = dataDirectory.resolve(configuredPath).normalize();
        if (!databasePath.startsWith(dataDirectory)) {
            throw new IllegalArgumentException("database.path はプラグインのデータフォルダ内を指定してください。");
        }
        return databasePath;
    }

    private static void bindNickname(PreparedStatement statement, UUID playerId, String nickname) throws SQLException {
        statement.setString(1, playerId.toString());
        statement.setString(2, nickname);
    }

    private static IllegalStateException databaseFailure(String operation, Exception cause) {
        return new IllegalStateException("ニックネームDBの" + operation + "に失敗しました。", cause);
    }
}