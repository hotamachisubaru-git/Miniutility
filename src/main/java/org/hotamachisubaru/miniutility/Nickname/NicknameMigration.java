package org.hotamachisubaru.miniutility.Nickname;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.hotamachisubaru.miniutility.MiniutilityLoader;

import java.io.File;
import java.util.UUID;
import java.util.Set;
import java.util.logging.Logger;

/**
 * nickname.yml から SQLite への一度きり移行を行う。
 */
public final class NicknameMigration {

    private final MiniutilityLoader plugin;
    private final NicknameDatabase nicknameDatabase;

    public NicknameMigration(MiniutilityLoader plugin, NicknameDatabase nicknameDatabase) {
        this.plugin = plugin;
        this.nicknameDatabase = nicknameDatabase;
    }

    public void migrateToDatabase() {
        File yamlFile = new File(plugin.getDataFolder(), "nickname.yml");
        Logger logger = plugin.getLogger();

        if (!yamlFile.exists()) {
            logger.info("nickname.yml が見つからないため、移行をスキップします。");
            return;
        }

        FileConfiguration yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
        Set<String> keys = yamlConfig.getKeys(false);
        if (keys == null || keys.isEmpty()) {
            logger.warning("nickname.yml の中身が空のため、移行をスキップします。");
            return;
        }

        nicknameDatabase.init();
        int migrated = 0;
        for (String key : keys) {
            String nickname = yamlConfig.getString(key);
            if (nickname == null || nickname.isBlank()) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                nicknameDatabase.saveNickname(uuid, nickname);
                migrated++;
            } catch (IllegalArgumentException ex) {
                logger.warning("UUIDとして解釈できないキーをスキップしました: " + key);
            }
        }

        File backupFile = new File(plugin.getDataFolder(), "nickname.yml.migrated");
        if (yamlFile.renameTo(backupFile)) {
            logger.info("nickname.yml の移行が完了しました。件数: " + migrated);
            logger.info("移行済みファイルを " + backupFile.getName() + " にリネームしました。");
        } else {
            logger.warning("nickname.yml のリネームに失敗しました。再起動時に再移行される可能性があります。");
        }
    }
}
