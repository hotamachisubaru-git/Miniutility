package org.hotamachisubaru.miniutility;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.hotamachisubaru.miniutility.bootstrap.MiniutilityInitializer;
import org.hotamachisubaru.miniutility.bootstrap.MiniutilityServices;
import org.hotamachisubaru.miniutility.death.PlayerDeathLocations;
import org.hotamachisubaru.miniutility.listeners.PlayerChatListener;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

/**
 * MiniutilityPlugin プラグインのエントリポイント。
 * <p>
 * 初期化は {@link MiniutilityInitializer} に委譲し、
 * ランタイム中は {@link MiniutilityServices} を介して機能にアクセスします。
 * 死亡地点の記録・取得は {@link PlayerDeathLocations} で管理します。
 */
public final class MiniutilityPlugin extends JavaPlugin {

    private final PlayerDeathLocations deathLocationStore = new PlayerDeathLocations();
    private @Nullable MiniutilityServices runtime;

    @Override
    public void onEnable() {
        runtime = new MiniutilityInitializer(this, deathLocationStore).enable();
    }

    @Override
    public void onDisable() {
        var activeRuntime = runtime;
        runtime = null;
        if (activeRuntime != null) {
            try {
                activeRuntime.close();
            } catch (RuntimeException exception) {
                getLogger().log(Level.SEVERE, "プラグインの終了処理に失敗しました。", exception);
            }
        }
    }

    // ─── 死亡地点 API ───────────────────────────────────────────

    /**
     * プレイヤーの死亡地点を記録します。
     */
    public void recordDeathLocation(UUID uniqueId, Location location) {
        deathLocationStore.record(
                Objects.requireNonNull(uniqueId, "uniqueId"),
                Objects.requireNonNull(location, "location")
        );
    }

    /**
     * プレイヤーの死亡地点を取得します。
     */
    public @Nullable Location getDeathLocation(UUID uniqueId) {
        return deathLocationStore.find(uniqueId).orElse(null);
    }

    /**
     * 死亡地点ストアの参照を返します。テスト用・内部用。
     */
    public PlayerDeathLocations getDeathLocationStore() {
        return deathLocationStore;
    }

    // ─── ランタイムアクセサ ─────────────────────────────────────

    /**
     * ニックネーマネージャーの参照を返します。
     */
    public PlayerNicknameService getNicknameManager() {
        return requireRuntime().getNicknameManager();
    }

    /**
     * チャットリスナーの参照を返します。
     */
    public PlayerChatListener getChatListener() {
        return requireRuntime().getChatListener();
    }

    private MiniutilityServices requireRuntime() {
        var activeRuntime = runtime;
        if (activeRuntime == null) {
            throw new IllegalStateException("Miniutility の実行環境が初期化されていません。");
        }
        return activeRuntime;
    }
}
