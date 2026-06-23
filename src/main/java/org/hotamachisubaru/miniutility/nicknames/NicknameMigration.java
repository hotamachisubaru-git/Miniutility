package org.hotamachisubaru.miniutility.nicknames;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NicknameMigration {

    private static final String LEGACY_FILE_NAME = "nickname.yml";

    private final Path dataDirectory;
    private final Logger logger;
    private final NicknameDatabase nicknameDatabase;

    public NicknameMigration(JavaPlugin plugin, NicknameDatabase nicknameDatabase) {
        Objects.requireNonNull(plugin, "plugin");
        this.dataDirectory = plugin.getDataFolder().toPath();
        this.logger = plugin.getLogger();
        this.nicknameDatabase = Objects.requireNonNull(nicknameDatabase, "nicknameDatabase");
    }

    public void migrateToDatabase() {
        Path source = dataDirectory.resolve(LEGACY_FILE_NAME);
        if (Files.notExists(source)) {
            return;
        }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(source.toFile());
        Map<UUID, String> nicknames = readValidNicknames(yaml);
        if (nicknames.isEmpty()) {
            return;
        }

        nicknameDatabase.saveAll(nicknames);
        archiveSourceFile(source, nicknames.size());
    }

    private Map<UUID, String> readValidNicknames(FileConfiguration yaml) {
        Map<UUID, String> nicknames = new HashMap<>();
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

    private void archiveSourceFile(Path source, int migratedCount) {
        Path archive = dataDirectory.resolve(LEGACY_FILE_NAME + ".migrated");
        try {
            Files.move(source, archive, StandardCopyOption.REPLACE_EXISTING);
            logger.info("nickname.yml から " + migratedCount
                    + " 件のニックネームを移行し、元ファイルを " + archive.getFileName() + " に退避しました。");
        } catch (IOException exception) {
            logger.log(Level.WARNING, "ニックネーム移行後のファイル退避に失敗しました。", exception);
            logger.info("nickname.yml から " + migratedCount + " 件のニックネームを移行しました。");
        }
    }
}
