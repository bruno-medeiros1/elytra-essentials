package org.bruno.elytraEssentials.helpers;

/**
 * Version format being used: MAJOR.MINOR.PATCH:
 * - MAJOR: Increment for breaking changes or overhauls.
 * - MINOR: Increment for new features that are backward-compatible.
 * - PATCH: Increment for backward-compatible bug fixes.
 */

public final class VersionHelper {

    private VersionHelper() {}

    // Compares two version strings to check if the latest version is newer.
    public static boolean isNewerVersion(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");

        for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (currentPart < latestPart) return true;
            if (currentPart > latestPart) return false;
        }
        return false;
    }
}