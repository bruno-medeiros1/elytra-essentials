package org.bruno.elytraEssentials.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.*;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class ElytraEssentialsPlaceholders extends PlaceholderExpansion {
    private final FlightHandler flightHandler;
    private final StatsHandler statsHandler;
    private final EffectsHandler effectsHandler;
    private final DatabaseHandler databaseHandler;
    private final ConfigHandler configHandler;
    private final PluginInfoHandler pluginInfoHandler;

    public ElytraEssentialsPlaceholders(FlightHandler flightHandler, StatsHandler statsHandler,EffectsHandler effectsHandler,
                                        DatabaseHandler databaseHandler, ConfigHandler configHandler, PluginInfoHandler pluginInfoHandler) {
        this.flightHandler = flightHandler;
        this.statsHandler = statsHandler;
        this.effectsHandler = effectsHandler;
        this.databaseHandler = databaseHandler;
        this.configHandler = configHandler;
        this.pluginInfoHandler = pluginInfoHandler;
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
        return pluginInfoHandler.getCurrentVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is good practice, it caches the results
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // Flight Time Placeholders
        if (identifier.equalsIgnoreCase("flight_time") || identifier.equalsIgnoreCase("flight_time_formatted")) {
            if (!configHandler.getIsTimeLimitEnabled()) {
                return "Disabled";
            }
            if (PermissionsHelper.playerBypassTimeLimit(player)) {
                return "Unlimited";
            }

            int flightTimeLeft = flightHandler.getCurrentFlightTime(player.getUniqueId());

            return identifier.equalsIgnoreCase("flight_time_formatted")
                    ? TimeHelper.formatFlightTime(flightTimeLeft)
                    : String.valueOf(flightTimeLeft);
        }

        // We get the stats once and use them for all stat-related placeholders
        var stats = statsHandler.getStats(player);
        if (stats == null) return "";

        switch (identifier.toLowerCase()) {
            // Flight Stats
            case "total_distance_km":
                return String.format("%.1f km", stats.getTotalDistance() / 1000.0);
            case "total_distance_blocks":
                return String.format("%.0f blocks", stats.getTotalDistance());
            case "total_flight_time":
                return TimeHelper.formatFlightTime((int) stats.getTotalTimeSeconds());
            case "longest_flight":
                return String.format("%.0f blocks", stats.getLongestFlight());
            case "average_speed":
                if (stats.getTotalTimeSeconds() > 0) {
                    double avgSpeedKmh = (stats.getTotalDistance() / stats.getTotalTimeSeconds()) * 3.6;
                    return String.format("%.1f km/h", avgSpeedKmh);
                }
                return "0.0 km/h";

            // Boost Stats
            case "boosts_used":
                return String.valueOf(stats.getBoostsUsed());
            case "super_boosts_used":
                return String.valueOf(stats.getSuperBoostsUsed());
            case "total_boosts":
                return String.valueOf(stats.getBoostsUsed() + stats.getSuperBoostsUsed());

            // Other Stats
            case "saves":
                return String.valueOf(stats.getPluginSaves());
            case "active_effect":
                String activeEffect = effectsHandler.getActiveEffect(player.getUniqueId());
                return (activeEffect != null) ? activeEffect : "None";
            case "effects_owned":
                try {
                    return String.valueOf(databaseHandler.getOwnedEffectKeys(player.getUniqueId()).size());
                } catch (SQLException e) {
                    return "Error";
                }
            case "effects_total":
                return String.valueOf(effectsHandler.getEffectsRegistry().size());
        }

        return null; // Let PlaceholderAPI know the placeholder was not found
    }
}