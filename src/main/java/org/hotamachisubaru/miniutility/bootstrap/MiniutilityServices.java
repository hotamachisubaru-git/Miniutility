package org.hotamachisubaru.miniutility.bootstrap;

import org.hotamachisubaru.miniutility.ReleaseUpdateNotifier;
import org.hotamachisubaru.miniutility.listeners.PlayerChatListener;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.ui.TrashInventorySessions;

import java.util.List;
import java.util.Objects;

/**
 * プラグイン起動後のランタイム状態を保持するクラス。
 * <p>
 * 終了処理時にニックネームの永続化とアップデートチェッカーの停止を行います。
 */
public final class MiniutilityServices implements AutoCloseable {

    private final PlayerNicknameService nicknameManager;
    private final PlayerChatListener chatListener;
    private final ReleaseUpdateNotifier updateChecker;
    private final TrashInventorySessions trashBoxSessions;
    private boolean closed;

    public MiniutilityServices(
            PlayerNicknameService nicknameManager,
            PlayerChatListener chatListener,
            ReleaseUpdateNotifier updateChecker
    ) {
        this(nicknameManager, chatListener, updateChecker, new TrashInventorySessions());
    }

    public MiniutilityServices(
            PlayerNicknameService nicknameManager,
            PlayerChatListener chatListener,
            ReleaseUpdateNotifier updateChecker,
            TrashInventorySessions trashBoxSessions
    ) {
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
        this.chatListener = Objects.requireNonNull(chatListener, "chatListener");
        this.updateChecker = Objects.requireNonNull(updateChecker, "updateChecker");
        this.trashBoxSessions = Objects.requireNonNull(trashBoxSessions, "trashBoxSessions");
    }

    // ─── 終了処理 ──────────────────────────────────────────────

    /**
     * プラグインのシャットダウン処理を実行します。
     * ニックネームデータを永続化し、アップデートチェッカーを停止します。
     */
    public void shutdown() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException failure = null;
        for (Runnable cleanup : List.<Runnable>of(
                updateChecker::stop, chatListener::clearPendingInputs,
                trashBoxSessions::clear, nicknameManager::persistAll)) {
            try {
                cleanup.run();
            } catch (RuntimeException cleanupFailure) {
                if (failure == null) {
                    failure = cleanupFailure;
                } else {
                    failure.addSuppressed(cleanupFailure);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    // ─── アクセサ ──────────────────────────────────────────────

    /**
     * ニックネーマネージャーの参照を返します。
     */
    public PlayerNicknameService getNicknameManager() {
        return nicknameManager;
    }

    /**
     * チャットリスナーの参照を返します。
     */
    public PlayerChatListener getChatListener() {
        return chatListener;
    }
}
