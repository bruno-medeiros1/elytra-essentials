package org.bruno.elytraEssentials.helpers;

public final class TimeHelper {

    private TimeHelper() {}

    /**
     * Dynamically formats the flight time based on its value.
     * - Shows seconds if < 60.
     * - Shows minutes and seconds if >= 60 and < 3600.
     * - Shows hours, minutes, and seconds if >= 3600.
     *
     * @param totalSeconds The total flight time in seconds.
     * @return A dynamically formatted string representing the flight time.
     */
    public static String formatFlightTime(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        } else if (totalSeconds < 3600) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format("%dm %ds", minutes, seconds);
        } else {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = totalSeconds % 60;
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        }
    }

    /**
     * Converts a duration in minutes to Minecraft ticks.
     *
     * @param minutes The number of minutes to convert.
     * @return The equivalent duration in server ticks.
     */
    public static long minutesToTicks(int minutes) {
        return (long) minutes * 60 * 20;
    }
}