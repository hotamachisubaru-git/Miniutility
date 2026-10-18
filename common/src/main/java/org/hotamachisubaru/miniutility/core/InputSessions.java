package org.hotamachisubaru.miniutility.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** セッションは同じ入力モードでも別のオブジェクト。古いチャット入力を再利用しません。 */
public final class InputSessions {
    public enum Mode { NICKNAME, COLORED_NICKNAME, EXPERIENCE }
    public static final class Session {
        private final Mode mode;
        private Session(Mode mode) { this.mode = mode; }
        public Mode mode() { return mode; }
    }
    private final ConcurrentHashMap<UUID, Session> pending = new ConcurrentHashMap<>();
    public Session begin(UUID player, Mode mode) { Session session = new Session(mode); pending.put(player, session); return session; }
    public Session get(UUID player) { return pending.get(player); }
    public boolean current(UUID player, Session session) { return pending.get(player) == session; }
    public void complete(UUID player, Session session) { pending.remove(player, session); }
    public void forget(UUID player) { pending.remove(player); }
    public void clear() { pending.clear(); }
}
