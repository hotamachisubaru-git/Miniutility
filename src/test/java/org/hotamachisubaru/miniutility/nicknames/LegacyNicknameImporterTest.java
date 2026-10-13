package org.hotamachisubaru.miniutility.nicknames;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

final class LegacyNicknameImporterTest {

    @TempDir
    private Path directory;

    @Test
    void migrationKeepsNewerDatabaseNamesAndPreviousArchives() throws Exception {
        UUID existing = UUID.randomUUID();
        UUID legacyOnly = UUID.randomUUID();
        Logger logger = Logger.getLogger("LegacyNicknameImporterTest");
        SqliteNicknameRepository database = new SqliteNicknameRepository(directory.resolve("nickname.db"), true, logger);
        database.saveNickname(existing, "current");
        Path source = directory.resolve("nickname.yml");
        String legacy = existing + ": old\n" + legacyOnly + ": ほしまち\nbroken: invalid\n";
        Files.writeString(source, legacy);
        Path previousArchive = directory.resolve("nickname.yml.migrated");
        Files.writeString(previousArchive, "previous migration evidence");

        LegacyNicknameImporter migration = new LegacyNicknameImporter(directory, logger, database);
        migration.migrateToDatabase();

        assertEquals(Map.of(existing, "current", legacyOnly, "ほしまち"), database.loadAll());
        assertFalse(Files.exists(source));
        assertEquals(legacy, Files.readString(directory.resolve("nickname.yml.migrated.1")));
        assertEquals("previous migration evidence", Files.readString(previousArchive));
        migration.migrateToDatabase();
        assertEquals(Map.of(existing, "current", legacyOnly, "ほしまち"), database.loadAll());
    }

    @Test
    void malformedYamlRemainsAvailableAndCannotModifyTheDatabase() throws Exception {
        Path source = directory.resolve("nickname.yml");
        String malformed = "nickname: [unclosed\n";
        Files.writeString(source, malformed);
        Logger logger = Logger.getLogger("LegacyNicknameImporterTest");
        SqliteNicknameRepository database = new SqliteNicknameRepository(directory.resolve("nickname.db"), true, logger);

        assertThrows(IllegalStateException.class,
                () -> new LegacyNicknameImporter(directory, logger, database).migrateToDatabase());
        assertEquals(malformed, Files.readString(source));
        assertFalse(Files.exists(directory.resolve("nickname.db")));
    }
}
