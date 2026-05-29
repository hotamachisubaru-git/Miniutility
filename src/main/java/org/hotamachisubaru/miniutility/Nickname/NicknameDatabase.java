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
import java.util.UUID;
import java.util.logging.Logger;

public final class NicknameDatabase {

    private final Logger logger;
    private final String dbUrl;

    public NicknameDatabase(MiniutilityLoader plugin) {
        this.logger = plugin.getLogger();
        String configuredPath = plugin.getConfig().getString("database.path", "nickname.db");
        File dbFile = new File(plugin.getDataFolder(), configuredPath);
        this.dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public void initialize() {
        try (Connection connection = DriverManager.getConnection(dbUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS nicknames (" +
                    "uuid TEXT PRIMARY KEY," +
                    "nickname TEXT NOT NULL" +
                    ")");
        } catch (SQLException exception) {
            logger.warning("ニックネームDBの初期化に失敗しました: " + exception.getMessage());
        }
    }

    public void saveNickname(UUID uniqueId, String nickname) {
        if (uniqueId == null || nickname == null) {
            return;
        }

        initialize();
        try (Connection connection = DriverManager.getConnection(dbUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT OR REPLACE INTO nicknames (uuid, nickname) VALUES (?, ?)")) {
            statement.setString(1, uniqueId.toString());
            statement.setString(2, nickname);
            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.warning("ニックネームの保存に失敗しました: " + exception.getMessage());
        }
    }

    public void deleteNickname(UUID uniqueId) {
        if (uniqueId == null) {
            return;
        }

        initialize();
        try (Connection connection = DriverManager.getConnection(dbUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM nicknames WHERE uuid = ?")) {
            statement.setString(1, uniqueId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            logger.warning("ニックネームの削除に失敗しました: " + exception.getMessage());
        }
    }

    public Map<UUID, String> loadAll() {
        initialize();

        Map<UUID, String> loaded = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(dbUrl);
             PreparedStatement statement = connection.prepareStatement("SELECT uuid, nickname FROM nicknames");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String uuid = resultSet.getString("uuid");
                try {
                    loaded.put(UUID.fromString(uuid), resultSet.getString("nickname"));
                } catch (IllegalArgumentException exception) {
                    logger.warning("不正なUUIDのニックネームデータをスキップしました: " + uuid);
                }
            }
        } catch (SQLException exception) {
            logger.warning("ニックネームの読み込みに失敗しました: " + exception.getMessage());
        }

        return loaded;
    }

    public void saveAll(Map<UUID, String> nicknameMap) {
        initialize();

        String sql = "INSERT OR REPLACE INTO nicknames (uuid, nickname) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Map.Entry<UUID, String> entry : nicknameMap.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setString(2, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            }
        } catch (SQLException exception) {
            logger.warning("ニックネームの一括保存に失敗しました: " + exception.getMessage());
        }
    }
}
