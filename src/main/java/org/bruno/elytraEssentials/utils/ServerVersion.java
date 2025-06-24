package org.bruno.elytraEssentials.utils;

import org.bukkit.Bukkit;

public enum ServerVersion {
    V_1_18,
    V_1_19,
    V_1_20,
    V_1_21,
    UNKNOWN;

    private static ServerVersion currentVersion;

    /**
     * Gets the current server version, determined once at startup.
     * @return The current ServerVersion enum constant.
     */
    public static ServerVersion getCurrent() {
        if (currentVersion != null)
            return currentVersion;

        String versionString = Bukkit.getBukkitVersion();

        if (versionString.contains("1.21")) {
            currentVersion = V_1_21;
        } else if (versionString.contains("1.20")) {
            currentVersion = V_1_20;
        } else if (versionString.contains("1.19")) {
            currentVersion = V_1_19;
        } else if (versionString.contains("1.18")) {
            currentVersion = V_1_18;
        } else {
            currentVersion = UNKNOWN;
        }
        return currentVersion;
    }
}