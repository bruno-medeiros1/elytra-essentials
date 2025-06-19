package org.bruno.elytraEssentials.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.TimeHelper;
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

        if (!plugin.getConfigHandlerInstance().getIsTimeLimitEnabled())
            return "Not Enabled";

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
                return TimeHelper.formatFlightTime(flightTime);
            }

            // For "flight_time", return the raw flight time in seconds as a string
            return flightTime + "s";
        }

        return null;
    }
}