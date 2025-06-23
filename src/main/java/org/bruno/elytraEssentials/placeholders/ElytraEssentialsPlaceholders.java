package org.bruno.elytraEssentials.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ElytraEssentialsPlaceholders extends PlaceholderExpansion {

    private final ElytraEssentials plugin;

    public ElytraEssentialsPlaceholders(ElytraEssentials plugin) {
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
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null)
            return "";

        //  Flight Time Placeholders
        if (identifier.equalsIgnoreCase("flight_time") || identifier.equalsIgnoreCase("flight_time_formatted")) {
            if (!plugin.getConfigHandlerInstance().getIsTimeLimitEnabled())
                return "Disabled";

            if (PermissionsHelper.PlayerBypassTimeLimit(player))
                return "Unlimited";

            int flightTimeLeft = plugin.getElytraFlightListener().GetAllActiveFlights().getOrDefault(player.getUniqueId(), 0);

            return identifier.equalsIgnoreCase("flight_time_formatted")
                    ? TimeHelper.formatFlightTime(flightTimeLeft)
                    : String.valueOf(flightTimeLeft);
        }

        // Stats Placeholders
        StatsHandler statsHandler = plugin.getStatsHandler();
        PlayerStats stats = statsHandler.getStats(player);
        if (stats == null) return "";

        return switch (identifier.toLowerCase()) {
            case "total_distance" -> String.format("%.1f", stats.getTotalDistance() / 1000.0) + "km";
            case "total_flight_time" -> TimeHelper.formatFlightTime((int) stats.getTotalTimeSeconds());
            case "longest_flight" -> String.format("%.0f", stats.getLongestFlight()) + " blocks";
            case "average_speed" -> {
                if (stats.getTotalTimeSeconds() > 0) {
                    double avgSpeedKmh = (stats.getTotalDistance() / stats.getTotalTimeSeconds()) * 3.6;
                    yield String.format("%.1f", avgSpeedKmh) + " km/h";
                }
                yield "0.0 km/h";
            }
            default -> null;
        };
    }
}