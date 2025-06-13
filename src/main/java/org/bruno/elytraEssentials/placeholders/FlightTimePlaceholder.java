package org.bruno.elytraEssentials.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlightTimePlaceholder extends PlaceholderExpansion {

    private final ElytraEssentials plugin;

    public FlightTimePlaceholder(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "elytraessentials";
    }

    @Override
    public @NotNull String getAuthor() {
        return "CodingMaestro";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This ensures the placeholder stays registered even if PlaceholderAPI is reloaded.
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // Check for the flight time placeholder
        if (identifier.equals("flight_time") || identifier.equals("flight_time_formatted")) {
            if (player.hasPermission("elytraessentials.bypass.timelimit") ||
                    player.hasPermission("elytraessentials.bypass.*") ||
                    player.hasPermission("elytraessentials.*")) {
                return "Unlimited Time";
            }

            // Fetch the flight time from the active flights map
            int flightTime = this.plugin.getElytraFlightListener()
                    .GetAllActiveFlights()
                    .getOrDefault(player.getUniqueId(), 0);

            // If the formatted placeholder is requested, format the time dynamically
            if (identifier.equals("flight_time_formatted")) {
                return formatFlightTimeDynamic(flightTime);
            }

            // For "flight_time", return the raw flight time in seconds as a string
            return flightTime + "s";
        }

        return null;
    }

    /**
     * Dynamically formats the flight time based on its value.
     * - Shows seconds if < 60.
     * - Shows minutes and seconds if >= 60 and < 3600.
     * - Shows hours, minutes, and seconds if >= 3600.
     *
     * @param totalSeconds The total flight time in seconds.
     * @return A dynamically formatted string representing the flight time.
     */
    private String formatFlightTimeDynamic(int totalSeconds) {
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
}