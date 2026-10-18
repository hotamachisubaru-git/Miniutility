package org.hotamachisubaru.miniutility.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/** Paperの非同期テレポートは利用可能な場合だけ呼び、Spigotでは同期APIを使います。 */
public final class ServerCompatibility {
    private static final Method ASYNC_TELEPORT = findAsyncTeleport();
    private ServerCompatibility() { }
    private static Method findAsyncTeleport() {
        try { return Player.class.getMethod("teleportAsync", Location.class); }
        catch (NoSuchMethodException absentOnSpigot) { return null; }
    }
    public static CompletableFuture<Boolean> teleport(Player player, Location location) {
        if (ASYNC_TELEPORT == null) return CompletableFuture.completedFuture(player.teleport(location));
        try {
            Object result = ASYNC_TELEPORT.invoke(player, location);
            if (result instanceof CompletableFuture<?> future) return future.thenApply(Boolean.TRUE::equals);
            return CompletableFuture.failedFuture(new IllegalStateException("Invalid Paper teleport result"));
        } catch (IllegalAccessException | InvocationTargetException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }
}
