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

public final class NicknameDatabase {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS nicknames (
                uuid TEXT PRIMARY KEY,
                nickname TEXT NOT NULL
            )
            """;
    private static final String UPSERT_NICKNAME_SQL = """
            INSERT INTO nicknames (uuid, nickname) VALUES (?, ?)
            ON CONFLICT(uuid) DO UPDATE SET nickname = excluded.nickname
            """;
    private static final String DELETE_NICKNAME_SQL = "DELETE FROM nicknames WHERE uuid = ?";
    private static final String SELECT_ALL_SQL = "SELECT uuid, nickname FROM nicknames";

    private final Logger logger;
    private final Path databasePath;
    private final String databaseUrl;
    private final boolean autoCreate;
    private volatile boolean initialized;

    public NicknameDatabase(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();

        Path dataDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        String configuredPath = plugin.getConfig().getString("database.path", "nickname.db");
        this.databasePath = dataDirectory.resolve(configuredPath).normalize();
        if (!databasePath.startsWith(dataDirectory)) {
            throw new IllegalArgumentException("database.path はプラグインのデータフォルダ内を指定してください。");
        }
        this.databaseUrl = "jdbc:sqlite:" + databasePath;
        this.autoCreate = plugin.getConfig().getBoolean("database.autoCreate", true);
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            if (!autoCreate && Files.notExists(databasePath)) {
                throw new IllegalStateException("ニックネームDBが存在せず、database.autoCreate が無効です。");
            }
            Files.createDirectories(databasePath.getParent());
            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE_TABLE_SQL);
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

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_NICKNAME_SQL)) {
            setNicknameParameters(statement, playerId, nickname);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseFailure("保存", exception);
        }
    }

    public void deleteNickname(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        ensureInitialized();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_NICKNAME_SQL)) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseFailure("削除", exception);
        }
    }

    public Map<UUID, String> loadAll() {
        ensureInitialized();
        Map<UUID, String> nicknames = new HashMap<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                addValidNickname(nicknames, resultSet);
            }
        } catch (SQLException exception) {
            throw databaseFailure("読み込み", exception);
        }
        return Map.copyOf(nicknames);
    }

    public void saveAll(Map<UUID, String> nicknames) {
        Objects.requireNonNull(nicknames, "nicknames");
        ensureInitialized();

        try (Connection connection = openConnection()) {
            executeBatch(connection, nicknames);
        } catch (SQLException exception) {
            throw databaseFailure("一括保存", exception);
        }
    }

    private void executeBatch(Connection connection, Map<UUID, String> nicknames) throws SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_NICKNAME_SQL)) {
            for (Map.Entry<UUID, String> entry : nicknames.entrySet()) {
                setNicknameParameters(
                        statement,
                        Objects.requireNonNull(entry.getKey(), "nickname playerId"),
                        Objects.requireNonNull(entry.getValue(), "nickname value")
                );
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        }
    }

    private void addValidNickname(Map<UUID, String> nicknames, ResultSet resultSet) throws SQLException {
        String serializedPlayerId = resultSet.getString("uuid");
        try {
            nicknames.put(UUID.fromString(serializedPlayerId), resultSet.getString("nickname"));
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

    private static void setNicknameParameters(
            PreparedStatement statement,
            UUID playerId,
            String nickname
    ) throws SQLException {
        statement.setString(1, playerId.toString());
        statement.setString(2, nickname);
    }

    private static void rollback(Connection connection, Exception originalFailure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            originalFailure.addSuppressed(rollbackFailure);
        }
    }

    private static IllegalStateException databaseFailure(String operation, Exception cause) {
        return new IllegalStateException("ニックネームDBの" + operation + "に失敗しました。", cause);
    }
}
