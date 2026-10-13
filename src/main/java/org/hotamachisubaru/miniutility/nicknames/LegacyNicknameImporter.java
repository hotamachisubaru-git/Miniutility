package org.hotamachisubaru.miniutility.nicknames;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Imports nickname.yml while keeping existing SQLite values and previous migration archives. */
public final class LegacyNicknameImporter {

    private static final String LEGACY_FILE_NAME = "nickname.yml";

    private final Path dataDirectory;
    private final Logger logger;
    private final SqliteNicknameRepository nicknameDatabase;

    public LegacyNicknameImporter(JavaPlugin plugin, SqliteNicknameRepository nicknameDatabase) {
        this(Objects.requireNonNull(plugin, "plugin").getDataFolder().toPath(),
                plugin.getLogger(), nicknameDatabase);
    }

    LegacyNicknameImporter(Path dataDirectory, Logger logger, SqliteNicknameRepository nicknameDatabase) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().normalize();
        this.logger = Objects.requireNonNull(logger, "logger");
        this.nicknameDatabase = Objects.requireNonNull(nicknameDatabase, "nicknameDatabase");
    }

    public void migrateToDatabase() {
        Path source = dataDirectory.resolve(LEGACY_FILE_NAME);
        if (Files.notExists(source)) {
            return;
        }
        Map<UUID, String> nicknames = readLegacyFile(source);
        if (nicknames.isEmpty()) {
            return;
        }
        // If archiving previously failed, replaying the legacy file must not revert newer DB names.
        nicknameDatabase.loadAll().keySet().forEach(nicknames::remove);
        nicknameDatabase.saveAll(nicknames);
        archiveSource(source, nicknames.size());
    }

    private Map<UUID, String> readLegacyFile(Path source) {
        var yaml = new YamlConfiguration();
        try {
            yaml.load(source.toFile());
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("旧ニックネームファイルの読み込みに失敗しました: " + source, exception);
        }
        return readValidNicknames(yaml);
    }

    private Map<UUID, String> readValidNicknames(FileConfiguration yaml) {
        var nicknames = new HashMap<UUID, String>();
        for (String serializedPlayerId : yaml.getKeys(false)) {
            String nickname = yaml.getString(serializedPlayerId);
            if (nickname == null || nickname.isBlank()) {
                continue;
            }
            try {
                nicknames.put(UUID.fromString(serializedPlayerId), nickname);
            } catch (IllegalArgumentException exception) {
                logger.warning("不正なUUIDのためニックネーム移行をスキップしました: " + serializedPlayerId);
            }
        }
        return nicknames;
    }

    private void archiveSource(Path source, int migratedCount) {
        for (int suffix = 0; suffix < Integer.MAX_VALUE; suffix++) {
            Path archive = dataDirectory.resolve(LEGACY_FILE_NAME + ".migrated" + (suffix == 0 ? "" : "." + suffix));
            try {
                Files.move(source, archive);
                logger.info("nickname.yml から " + migratedCount + " 件のニックネームを移行し、元ファイルを "
                        + archive.getFileName() + " に退避しました。");
                return;
            } catch (FileAlreadyExistsException archiveAlreadyExists) {
                // Keep earlier migration evidence and try a new archive name.
            } catch (IOException exception) {
                logger.log(Level.WARNING, "ニックネーム移行後のファイル退避に失敗しました。", exception);
                return;
            }
        }
        logger.warning("nickname.yml の退避先を確保できませんでした。元ファイルを保持します。");
    }
}