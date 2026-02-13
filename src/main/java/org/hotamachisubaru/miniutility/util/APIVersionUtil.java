package org.hotamachisubaru.miniutility.util;

import org.bukkit.Bukkit;

public final class APIVersionUtil {

    private APIVersionUtil() {
    }

    public static int getMajorVersion() {
        String version = Bukkit.getBukkitVersion(); // e.g. 1.21.1-R0.1-SNAPSHOT
        String[] parts = version.split("-")[0].split("\\.");
        try {
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
            if (parts.length == 1) {
                return Integer.parseInt(parts[0]);
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isAtLeast(int majorVersion) {
        return getMajorVersion() >= majorVersion;
    }

    public static boolean isModern() {
        return isAtLeast(20);
    }
}
