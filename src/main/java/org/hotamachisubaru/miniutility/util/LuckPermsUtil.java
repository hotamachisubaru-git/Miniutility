package org.hotamachisubaru.miniutility.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.entity.Player;

public final class LuckPermsUtil {

    private LuckPermsUtil() {
    }

    public static String safePrefix(Player player) {
        if (player == null) {
            return "";
        }

        try {
            LuckPerms api = LuckPermsProvider.get();
            var metaData = api.getPlayerAdapter(Player.class).getMetaData(player);
            return metaData.getPrefix() == null ? "" : metaData.getPrefix();
        } catch (IllegalStateException | NoClassDefFoundError ignored) {
            return "";
        } catch (Throwable ignored) {
            return "";
        }
    }
}

