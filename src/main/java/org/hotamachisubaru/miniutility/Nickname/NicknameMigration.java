package org.hotamachisubaru.miniutility.Nickname;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public final class NicknameMigration {

    private final JavaPlugin plugin;
    private final NicknameDatabase nicknameDatabase;

    public NicknameMigration(JavaPlugin plugin, NicknameDatabase nicknameDatabase) {
        this.plugin = plugin;
        this.nicknameDatabase = nicknameDatabase;
    }

    public void migrateToDatabase() {
        File yamlFile = new File(plugin.getDataFolder(), "nickname.yml");
        Logger logger = plugin.getLogger();

        if (!yamlFile.exists()) {
            return;
        }

        FileConfiguration yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
        Set<String> keys = yamlConfig.getKeys(false);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        int migratedCount = 0;
        for (String uuidString : keys) {
            String nickname = yamlConfig.getString(uuidString);
            if (nickname == null || nickname.isBlank()) {
                continue;
            }

            try {
                nicknameDatabase.saveNickname(UUID.fromString(uuidString), nickname);
                migratedCount++;
            } catch (IllegalArgumentException exception) {
                logger.warning("不正なUUIDのためニックネーム移行をスキップしました: " + uuidString);
            }
        }

        if (migratedCount == 0) {
            return;
        }

        File migratedFile = new File(plugin.getDataFolder(), "nickname.yml.migrated");
        try {
            Files.move(yamlFile.toPath(), migratedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("nickname.yml から " + migratedCount + " 件のニックネームを移行し、元ファイルを " + migratedFile.getName() + " に退避しました。");
        } catch (IOException exception) {
            logger.warning("ニックネーム移行後のファイル退避に失敗しました: " + exception.getMessage());
            logger.info("nickname.yml から " + migratedCount + " 件のニックネームを移行しました。");
        }
    }
}
