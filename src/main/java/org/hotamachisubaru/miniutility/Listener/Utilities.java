package org.hotamachisubaru.miniutility.Listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Objects;

/**
 * 旧統合リスナー互換用の委譲クラス。
 */
@Deprecated
public final class Utilities implements Listener {

    private final Menu menuListener;
    private final NicknameListener nicknameListener;
    private final TrashListener trashListener;

    public Utilities(Menu menuListener, NicknameListener nicknameListener, TrashListener trashListener) {
        this.menuListener = Objects.requireNonNull(menuListener, "menuListener");
        this.nicknameListener = Objects.requireNonNull(nicknameListener, "nicknameListener");
        this.trashListener = Objects.requireNonNull(trashListener, "trashListener");
    }

    @EventHandler(ignoreCancelled = true)
    public void handleInventoryClick(InventoryClickEvent event) {
        menuListener.handleInventoryClick(event);
        if (event.isCancelled()) {
            return;
        }

        nicknameListener.onNicknameMenuClick(event);
        if (event.isCancelled()) {
            return;
        }

        trashListener.onTrashClick(event);
    }
}
