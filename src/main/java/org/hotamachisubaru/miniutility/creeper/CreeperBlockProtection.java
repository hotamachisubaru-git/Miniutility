package org.hotamachisubaru.miniutility.creeper;


/**
 * クリーパーのブロック破壊防止を管理するサービス。
 * <p>
 * 初期状態は設定ファイルから受け取り、切り替えと参照の整合性を保ちます。
 */
public final class CreeperBlockProtection {

    private volatile boolean enabled;

    public CreeperBlockProtection(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * クリーパー保護の状態を切り替え、新しい状態を返します。
     */
    public synchronized boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    /**
     * クリーパー保護が有効かどうかを返します。
     */
    public boolean isEnabled() {
        return enabled;
    }
}
