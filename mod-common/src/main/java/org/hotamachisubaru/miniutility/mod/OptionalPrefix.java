package org.hotamachisubaru.miniutility.mod;

import java.util.UUID;

public final class OptionalPrefix {
    private OptionalPrefix() { }
    public static String lookup(UUID player) {
        try { return Bridge.lookup(player); }
        catch (NoClassDefFoundError | IllegalStateException missing) { return ""; }
    }
    private static final class Bridge {
        static String lookup(UUID player) {
            var user = net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(player);
            if (user == null) return "";
            String prefix = user.getCachedData().getMetaData().getPrefix();
            return prefix == null ? "" : prefix;
        }
    }
}
