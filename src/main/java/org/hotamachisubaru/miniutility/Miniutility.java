package org.hotamachisubaru.miniutility;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.hotamachisubaru.miniutility.bootstrap.PluginBootstrap;
import org.hotamachisubaru.miniutility.bootstrap.PluginRuntime;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;
import org.hotamachisubaru.miniutility.death.DeathLocationStore;
import org.hotamachisubaru.miniutility.listeners.ChatListener;
import org.hotamachisubaru.miniutility.nicknames.NicknameManager;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class Miniutility extends JavaPlugin {

    private final DeathLocationStore deathLocationStore = new DeathLocationStore();
    private PluginRuntime runtime;

    @Override
    public void onEnable() {
        runtime = new PluginBootstrap(this, deathLocationStore).enable();
    }

    @Override
    public void onDisable() {
        PluginRuntime activeRuntime = runtime;
        runtime = null;
        if (activeRuntime != null) {
            try {
                activeRuntime.shutdown();
            } catch (RuntimeException exception) {
                getLogger().log(Level.SEVERE, "プラグインの終了処理に失敗しました。", exception);
            }
        }
    }

    public void recordDeathLocation(UUID uniqueId, Location location) {
        deathLocationStore.record(
                Objects.requireNonNull(uniqueId, "uniqueId"),
                Objects.requireNonNull(location, "location")
        );
    }

    public Location getDeathLocation(UUID uniqueId) {
        return deathLocationStore.find(uniqueId).orElse(null);
    }

    public DeathLocationStore getDeathLocationStore() {
        return deathLocationStore;
    }

    public NicknameManager getNicknameManager() {
        return requireRuntime().getNicknameManager();
    }

    public ChatListener getChatListener() {
        return requireRuntime().getChatListener();
    }

    public CreeperProtectionService getCreeperProtectionService() {
        return requireRuntime().getCreeperProtectionService();
    }

    private PluginRuntime requireRuntime() {
        if (runtime == null) {
            throw new IllegalStateException("Miniutility の実行環境が初期化されていません。");
        }
        return runtime;
    }
}
