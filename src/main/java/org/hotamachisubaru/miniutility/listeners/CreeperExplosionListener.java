package org.hotamachisubaru.miniutility.listeners;

import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.hotamachisubaru.miniutility.creeper.CreeperBlockProtection;

import java.util.Objects;

/**
 * クリーパーの爆発イベントを監視し、保護が有効な場合に
 * ブロック破壊を防止するリスナー。
 */
public final class CreeperExplosionListener implements Listener {

    private final CreeperBlockProtection creeperProtectionService;

    public CreeperExplosionListener(CreeperBlockProtection creeperProtectionService) {
        this.creeperProtectionService = Objects.requireNonNull(
                creeperProtectionService,
                "creeperProtectionService"
        );
    }

    /**
     * クリーパーの爆発イベントを処理します。
     * <p>
     * 保護が有効で、かつ爆発元がクリーパーの場合、
     * ブロック破壊リストを空にして破壊を無効化します。
     * 爆発準備イベントや爆発自体をキャンセルしないため、ダメージやノックバックは維持します。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (event.isCancelled() || !creeperProtectionService.isEnabled()
                || !(event.getEntity() instanceof Creeper)) {
            return;
        }
        event.blockList().clear();
    }
}
