package org.hotamachisubaru.miniutility.Nickname;

import org.hotamachisubaru.miniutility.MiniutilityLoader;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ニックネーム永続化レイヤー。
 */
public final class NicknameDatabase {

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS nicknames (uuid TEXT PRIMARY KEY, nickname TEXT NOT NULL)";
    private static final String UPSERT_SQL =
            "INSERT OR REPLACE INTO nicknames (uuid, nickname) VALUES (?, ?)";

    private final Logger logger;
    private final File dataFolder;
    private final String jdbcUrl;

    public NicknameDatabase(MiniutilityLoader plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.dataFolder = plugin.getDataFolder();
        this.jdbcUrl = "jdbc:sqlite:" + new File(dataFolder, "nickname.db").getAbsolutePath();
    }

    public void init() {
        ensureDataFolder();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE_SQL);
        } catch (SQLException e) {
            logger.warning("ニックネームDBの初期化に失敗しました: " + e.getMessage());
        }
    }

    public void saveNickname(UUID uuid, String nickname) {
        if (uuid == null || nickname == null) {
            return;
        }

        init();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, nickname);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.warning("ニックネームの保存に失敗しました: " + e.getMessage());
        }
    }

    public Optional<String> loadNickname(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }

        init();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT nickname FROM nicknames WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("nickname"));
                }
            }
        } catch (SQLException e) {
            logger.warning("ニックネームの取得に失敗しました: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Map<UUID, String> loadAllNicknames() {
        init();
        Map<UUID, String> nicknames = new HashMap<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT uuid, nickname FROM nicknames");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String rawUuid = resultSet.getString("uuid");
                String nickname = resultSet.getString("nickname");
                try {
                    nicknames.put(UUID.fromString(rawUuid), nickname);
                } catch (IllegalArgumentException ex) {
                    logger.warning("無効なUUIDをスキップしました: " + rawUuid);
                }
            }
        } catch (SQLException e) {
            logger.warning("ニックネーム一覧の読み込みに失敗しました: " + e.getMessage());
        }

        return nicknames;
    }

    public void deleteNickname(UUID uuid) {
        if (uuid == null) {
            return;
        }

        init();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM nicknames WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.warning("ニックネームの削除に失敗しました: " + e.getMessage());
        }
    }

    public void saveAll(Map<UUID, String> nicknames) {
        if (nicknames == null || nicknames.isEmpty()) {
            return;
        }

        init();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
                for (Map.Entry<UUID, String> entry : nicknames.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    statement.setString(1, entry.getKey().toString());
                    statement.setString(2, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    logger.warning("ニックネーム保存失敗時のロールバックに失敗しました: " + rollbackException.getMessage());
                }
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.warning("ニックネームの一括保存に失敗しました: " + e.getMessage());
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void ensureDataFolder() {
        if (dataFolder.exists()) {
            return;
        }
        if (!dataFolder.mkdirs()) {
            logger.warning("データフォルダの作成に失敗しました: " + dataFolder.getAbsolutePath());
        }
    }
}
