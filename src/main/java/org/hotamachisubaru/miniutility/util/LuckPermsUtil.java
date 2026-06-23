package org.hotamachisubaru.miniutility.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class LuckPermsUtil {

    private LuckPermsUtil() {
    }

    public static String prefixOrEmpty(Player player) {
        Objects.requireNonNull(player, "player");
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            String prefix = luckPerms.getPlayerAdapter(Player.class).getMetaData(player).getPrefix();
            return Objects.requireNonNullElse(prefix, "");
        } catch (IllegalStateException | NoClassDefFoundError ignored) {
            // LuckPerms is an optional dependency and may be unavailable during startup or shutdown.
            return "";
        }
    }
}

