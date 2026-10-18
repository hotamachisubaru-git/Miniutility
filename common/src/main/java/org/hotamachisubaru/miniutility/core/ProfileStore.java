package org.hotamachisubaru.miniutility.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** MOD用のワールド単位の保存。書込み完了後にメモリを更新し、失敗時は元の状態を維持します。 */
public final class ProfileStore {
    private final Path file;
    private Properties values = new Properties();
    public ProfileStore(Path file) throws IOException { this.file = file.toAbsolutePath(); reload(); }
    public synchronized void reload() throws IOException {
        Properties next = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { next.load(reader); }
            catch (IllegalArgumentException failure) { throw new IOException("保存データが不正です: " + file, failure); }
            if (!"1".equals(next.getProperty("schema"))) throw new IOException("未対応の保存形式: " + file);
        }
        values = next;
    }
    public synchronized String nickname(UUID player, String fallback) { return values.getProperty(player + ".nickname", fallback); }
    public synchronized boolean prefix(UUID player) { return Boolean.parseBoolean(values.getProperty(player + ".prefix", "true")); }
    public synchronized boolean protectedBlocks() { return Boolean.parseBoolean(values.getProperty("creeper-protection", "true")); }
    public synchronized void setNickname(UUID player, String nickname) throws IOException { update(player + ".nickname", nickname); }
    public synchronized void prefix(UUID player, boolean enabled) throws IOException { update(player + ".prefix", Boolean.toString(enabled)); }
    public synchronized void protectedBlocks(boolean enabled) throws IOException { update("creeper-protection", Boolean.toString(enabled)); }
    private void update(String key, String value) throws IOException {
        Properties next = new Properties(); next.putAll(values); next.setProperty("schema", "1");
        if (value == null) next.remove(key); else next.setProperty(key, value);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "profiles-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { next.store(writer, "Miniutility profiles"); }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            values = next;
        } finally { Files.deleteIfExists(temporary); }
    }
}
