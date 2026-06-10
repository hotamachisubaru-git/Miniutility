package org.hotamachisubaru.miniutility;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.hotamachisubaru.miniutility.Listener.Chat;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.bootstrap.PluginBootstrap;
import org.hotamachisubaru.miniutility.bootstrap.PluginRuntime;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;
import org.hotamachisubaru.miniutility.death.DeathLocationStore;

import java.util.UUID;

public final class Miniutility extends JavaPlugin {

    private final DeathLocationStore deathLocationStore = new DeathLocationStore();
    private PluginRuntime runtime;

    @Override
    public void onEnable() {
        runtime = new PluginBootstrap(this, deathLocationStore).enable();
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.shutdown();
            runtime = null;
        }
    }

    public void recordDeathLocation(UUID uniqueId, Location location) {
        deathLocationStore.record(uniqueId, location);
    }

    public Location getDeathLocation(UUID uniqueId) {
        return deathLocationStore.get(uniqueId);
    }

    public DeathLocationStore getDeathLocationStore() {
        return deathLocationStore;
    }

    public NicknameManager getNicknameManager() {
        return requireRuntime().getNicknameManager();
    }

    public Chat getChatListener() {
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
