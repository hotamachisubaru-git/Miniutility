package org.hotamachisubaru.miniutility.nicknames;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class SqliteNicknameRepositoryTest {

    @TempDir
    private Path directory;

    @Test
    void createsNestedDatabaseAndPersistsUnicodeUpsertsAndDeletesAcrossReopening() {
        Path path = directory.resolve("nested/nickname.db");
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        SqliteNicknameRepository database = database(path, true);
        database.saveNickname(owner, "ほしまち");
        database.saveAll(Map.of(owner, "§bすいせい", other, "みこ"));

        SqliteNicknameRepository reopened = database(path, false);
        assertEquals(Map.of(owner, "§bすいせい", other, "みこ"), reopened.loadAll());
        reopened.deleteNickname(owner);
        reopened.deleteNickname(owner);
        assertEquals(Map.of(other, "みこ"), database(path, false).loadAll());
        assertThrows(UnsupportedOperationException.class, () -> reopened.loadAll().clear());
    }

    @Test
    void disabledAutoCreationDoesNotCreateAMissingDatabaseOrItsDirectory() {
        Path path = directory.resolve("missing/nickname.db");

        assertThrows(IllegalStateException.class, () -> database(path, false).initialize());
        assertFalse(Files.exists(path));
        assertFalse(Files.exists(path.getParent()));
    }

    @Test
    void invalidBatchCannotCreateOrPartiallyModifyTheDatabase() {
        Path path = directory.resolve("nickname.db");
        SqliteNicknameRepository database = database(path, true);
        var invalid = new HashMap<UUID, String>();
        invalid.put(UUID.randomUUID(), "valid");
        invalid.put(UUID.randomUUID(), null);

        assertThrows(NullPointerException.class, () -> database.saveAll(invalid));
        assertFalse(Files.exists(path));
        UUID existing = UUID.randomUUID();
        database.saveNickname(existing, "before");
        assertThrows(NullPointerException.class, () -> database.saveAll(invalid));
        assertEquals(Map.of(existing, "before"), database.loadAll());
    }

    @Test
    void sqlFailureRollsBackTheEntireBatchIncludingEarlierSuccessfulRows() throws Exception {
        Path path = directory.resolve("nickname.db");
        SqliteNicknameRepository database = database(path, true);
        UUID existing = UUID.randomUUID();
        database.saveNickname(existing, "before");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TRIGGER reject_test_nickname BEFORE INSERT ON nicknames
                    WHEN NEW.nickname = 'reject'
                    BEGIN SELECT RAISE(ABORT, 'test batch rejection'); END
                    """);
        }

        assertThrows(IllegalStateException.class, () -> database.saveAll(Map.of(
                existing, "after", UUID.randomUUID(), "reject", UUID.randomUUID(), "new"
        )));

        assertEquals(Map.of(existing, "before"), database(path, false).loadAll());
    }

    @Test
    void skipsCorruptRowsWithoutLosingValidStoredNames() throws Exception {
        Path path = directory.resolve("nickname.db");
        Logger logger = mock(Logger.class);
        SqliteNicknameRepository database = new SqliteNicknameRepository(path, true, logger);
        UUID owner = UUID.randomUUID();
        database.saveNickname(owner, "Alice");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO nicknames VALUES ('broken-uuid', 'broken')");
            statement.executeUpdate("INSERT INTO nicknames VALUES (NULL, 'missing')");
        }

        assertEquals(Map.of(owner, "Alice"), database.loadAll());
        verify(logger, times(2)).warning(anyString());
    }

    @Test
    void retainsTheExistingDataFolderPathBoundaryAndAcceptsAbsolutePathsWithinIt() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("SqliteNicknameRepositoryTest"));
        config.set("database.path", directory.resolve("absolute/nickname.db").toString());

        new SqliteNicknameRepository(plugin).initialize();

        assertTrue(Files.exists(directory.resolve("absolute/nickname.db")));
        config.set("database.path", directory.getParent().resolve("outside.db").toString());
        assertThrows(IllegalArgumentException.class, () -> new SqliteNicknameRepository(plugin));
        config.set("database.path", "../outside.db");
        assertThrows(IllegalArgumentException.class, () -> new SqliteNicknameRepository(plugin));
    }

    private SqliteNicknameRepository database(Path path, boolean autoCreate) {
        return new SqliteNicknameRepository(path, autoCreate, Logger.getLogger("SqliteNicknameRepositoryTest"));
    }
}
