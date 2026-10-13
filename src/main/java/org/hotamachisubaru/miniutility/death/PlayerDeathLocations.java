package org.hotamachisubaru.miniutility.death;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * プレイヤーの死亡地点を管理するストア。
 * <p>
 * World への参照を保持せず、ワールド UUID と不変の座標を保存します。
 * ワールドがアンロードされている間は取得できず、再ロード後は再び取得できます。
 * Bukkit のワールド検索を使用するため、記録・取得はサーバースレッドから呼び出します。
 */
public final class PlayerDeathLocations {

    private final Map<UUID, DeathLocation> deathLocations = new ConcurrentHashMap<>();
    private final Function<UUID, @Nullable World> worldResolver;

    public PlayerDeathLocations() {
        this(Bukkit::getWorld);
    }

    PlayerDeathLocations(Function<UUID, @Nullable World> worldResolver) {
        this.worldResolver = Objects.requireNonNull(worldResolver, "worldResolver");
    }

    /**
     * プレイヤーの死亡地点を記録します。
     * <p>
     * 既存の記録があれば上書きされます。
     *
     * @param playerId   プレイヤーの UUID
     * @param location   死亡地点のロケーション
     * @throws NullPointerException playerId または location が null の場合
     */
    public void record(UUID playerId, Location location) {
        Objects.requireNonNull(location, "location");
        World world = location.getWorld();
        deathLocations.put(
                Objects.requireNonNull(playerId, "playerId"),
                new DeathLocation(world == null ? null : world.getUID(), location.getX(), location.getY(),
                        location.getZ(), location.getYaw(), location.getPitch())
        );
    }

    /**
     * プレイヤーの死亡地点を取得します。
     * <p>
     * 記録がない場合や、保存したワールドが未ロードの場合は {@link Optional#empty()} を返します。
     * 取得したロケーションを変更しても、保存済みの座標には影響しません。
     *
     * @param playerId プレイヤーの UUID
     * @return 死亡地点を含む Optional
     * @throws NullPointerException playerId が null の場合
     */
    public Optional<Location> find(UUID playerId) {
        DeathLocation location = deathLocations.get(Objects.requireNonNull(playerId, "playerId"));
        if (location == null) {
            return Optional.empty();
        }
        UUID worldId = location.worldId();
        World world = worldId == null ? null : worldResolver.apply(worldId);
        if (worldId != null && world == null) {
            return Optional.empty();
        }
        return Optional.<Location>of(new Location(world, location.x(), location.y(), location.z(),
                location.yaw(), location.pitch()));
    }

    private record DeathLocation(@Nullable UUID worldId, double x, double y, double z, float yaw, float pitch) { }
}
