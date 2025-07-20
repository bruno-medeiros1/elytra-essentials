package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.helpers.*;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bruno.elytraEssentials.utils.StatType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatsHandler {

    private static final double METERS_IN_ONE_KILOMETER = 1000.0;
    private static final double KMH_CONVERSION_FACTOR = 3.6;

    private final DatabaseHandler databaseHandler;
    private final FoliaHelper foliaHelper;
    private final Logger logger;
    private final MessagesHelper messagesHelper;
    private final EffectsHandler effectsHandler;

    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();
    private final Set<UUID> glidingPlayers = new HashSet<>();
    private record PlayerRanks(int distanceRank, int timeRank, int longestFlightRank) {}
    private CancellableTask task;

    // Constructor updated with new dependencies
    public StatsHandler(Logger logger, DatabaseHandler databaseHandler, FoliaHelper foliaHelper, MessagesHelper messagesHelper, EffectsHandler effectsHandler) {
        this.logger = logger;
        this.databaseHandler = databaseHandler;
        this.foliaHelper = foliaHelper;
        this.messagesHelper = messagesHelper;
        this.effectsHandler = effectsHandler;
    }

    /**
     * Asynchronously fetches all necessary stats and ranks for a player and displays them to the sender.
     * This is the new entry point called by the command.
     */
    public void fetchAndDisplayStats(CommandSender sender, OfflinePlayer target) {
        messagesHelper.sendCommandSenderMessage(sender, "&eFetching stats for " + target.getName() + "...");

        foliaHelper.runAsyncTask(() -> {
            try {
                PlayerStats targetStats;
                if (target.isOnline()) {
                    targetStats = getStats(target.getPlayer()); // Get live data from cache
                } else {
                    targetStats = databaseHandler.getPlayerStats(target.getUniqueId()); // Get last saved data from DB
                }

                // Ranks always come from the database as they are relative to all players.
                int distanceRank = databaseHandler.getPlayerRank(target.getUniqueId(), "total_distance");
                int timeRank = databaseHandler.getPlayerRank(target.getUniqueId(), "total_time_seconds");
                int longestFlightRank = databaseHandler.getPlayerRank(target.getUniqueId(), "longest_flight");
                PlayerRanks ranks = new PlayerRanks(distanceRank, timeRank, longestFlightRank);

                // Switch back to main thread to display the stats.
                foliaHelper.runTaskOnMainThread(() -> {
                    String titlePrefix = (sender instanceof Player p && p.getUniqueId().equals(target.getUniqueId()))
                            ? "Your" : target.getName() + "'s";
                    displayStats(sender, targetStats, ranks, titlePrefix);
                });

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Could not fetch stats for " + target.getName(), e);
                foliaHelper.runTaskOnMainThread(() ->
                        messagesHelper.sendCommandSenderMessage(sender, "&cAn error occurred while fetching stats."));
            }
        });
    }

    public void loadPlayerStats(Player player) {
        foliaHelper.runAsyncTask(() -> {
            try {
                PlayerStats stats = databaseHandler.getPlayerStats(player.getUniqueId());
                statsCache.put(player.getUniqueId(), stats);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Could not load stats for player " + player.getName(), e);
            }
        });
    }

    public void savePlayerStats(Player player) {
        PlayerStats stats = statsCache.remove(player.getUniqueId());
        glidingPlayers.remove(player.getUniqueId());

        if (stats != null) {
            foliaHelper.runAsyncTask(() -> {
                try {
                    databaseHandler.savePlayerStats(stats);
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Could not save stats for player " + player.getName(), e);
                }
            });
        }
    }

    // This is now fully asynchronous to prevent lag on shutdown/reload
    public void saveAllOnlinePlayers() {
       messagesHelper.sendDebugMessage("Saving stats asynchronously for all online players...");
        foliaHelper.runAsyncTask(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerStats stats = statsCache.get(player.getUniqueId());
                if (stats != null) {
                    try {
                        databaseHandler.savePlayerStats(stats);
                    } catch (SQLException e) {
                        logger.log(Level.SEVERE, "Failed to save stats for " + player.getName() + " during async save-all.", e);
                    }
                }
            }
           messagesHelper.sendDebugMessage("Finished async saving of all player stats.");
        });
    }

    public PlayerStats getStats(Player player) {
        return statsCache.getOrDefault(player.getUniqueId(), new PlayerStats(player.getUniqueId()));
    }

    /**
     * Retrieves a specific statistic value for a player.
     * @param player The player to get the stat for.
     * @param type The type of statistic to retrieve.
     * @return The value of the statistic as a double.
     */
    public double getStatValue(Player player, StatType type) {
        PlayerStats stats = getStats(player);
        if (stats == null) return 0.0;

        return switch (type) {
            case TOTAL_DISTANCE -> stats.getTotalDistance();
            case LONGEST_FLIGHT -> stats.getLongestFlight();
            case TOTAL_FLIGHT_TIME -> stats.getTotalTimeSeconds();
            case BOOSTS_USED -> stats.getBoostsUsed();
            case SUPER_BOOSTS_USED -> stats.getSuperBoostsUsed();
            case SAVES -> stats.getPluginSaves();
            default -> 0.0;
        };
    }

    public void setGliding(Player player, boolean isGliding) {
        if (isGliding) {
            glidingPlayers.add(player.getUniqueId());
        } else {
            glidingPlayers.remove(player.getUniqueId());
        }
    }

    public void start() {
        if (this.task != null) return;

        // Use the Folia-safe global timer
        this.task = foliaHelper.runTaskTimerGlobal(this::glidingTimeTracker, 20L, 20L);
    }

    public void shutdown() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }

        // Save any remaining data on shutdown
        saveAllOnlinePlayers();
    }

    public void displayTopStats(CommandSender sender, String category) {
        final String dbColumn;
        final String title;
        final String format;

        // Determine which statistic to query based on the category
        switch (category) {
            case "distance" -> {
                dbColumn = "total_distance";
                title = "Top Distance Flown";
                format = "§e#%d §f%s §7- §b%.1f km";
            }
            case "time" -> {
                dbColumn = "total_time_seconds";
                title = "Top Flight Time";
                format = "§e#%d §f%s §7- §b%s";
            }
            case "longest" -> {
                dbColumn = "longest_flight";
                title = "Longest Single Flights";
                format = "§e#%d §f%s §7- §b%.0f blocks";
            }
            default -> {
                messagesHelper.sendCommandSenderMessage(sender, "&cInvalid category. Use: distance, time, longest");
                return;
            }
        }

        // Run the database query asynchronously using the FoliaHelper
        foliaHelper.runAsyncTask(() -> {
            try {
                Map<UUID, Double> topData = databaseHandler.getTopStats(dbColumn, 5); // LEADERBOARD_LIMIT
                List<String> formattedMessages = new ArrayList<>();

                if (topData.isEmpty()) {
                    formattedMessages.add("§7No statistics available for this category yet.");
                } else {
                    int rank = 1;
                    for (Map.Entry<UUID, Double> entry : topData.entrySet()) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                        String playerName = (player.getName() != null) ? player.getName() : "Unknown";
                        Double value = entry.getValue();

                        String formattedLine = switch (category) {
                            case "time" -> String.format(format, rank, playerName, TimeHelper.formatFlightTime(value.intValue()));
                            case "distance" -> String.format(format, rank, playerName, value / 1000.0);
                            case "longest" -> String.format(format, rank, playerName, value);
                            default -> "";
                        };
                        if (!formattedLine.isBlank()) {
                            formattedMessages.add(formattedLine);
                            rank++;
                        }
                    }
                }

                // Send the formatted messages back on the main server thread
                foliaHelper.runTaskOnMainThread(() -> {
                    sender.sendMessage("§6--- " + title + " ---");
                    for (String message : formattedMessages) {
                        sender.sendMessage(message);
                    }
                    sender.sendMessage("§6-------------------------------------");
                });

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "An error occurred while fetching the leaderboard.", e);
                foliaHelper.runTaskOnMainThread(() ->
                        messagesHelper.sendCommandSenderMessage(sender, "&cAn error occurred while fetching the leaderboard."));
            }
        });
    }

    private void glidingTimeTracker() {
        for (UUID uuid : glidingPlayers) {
            PlayerStats stats = statsCache.get(uuid);
            if (stats != null) {
                stats.addTime(1); // Add 1 second to their total time
            }
        }
    }

    /**
     * Private helper method to format and send the stats message.
     * Moved from StatsCommand.
     */
    private void displayStats(CommandSender sender, PlayerStats stats, PlayerRanks ranks, String titlePrefix) {
        if (stats == null) {
            messagesHelper.sendCommandSenderMessage(sender, "&cCould not load stats for this player.");
            return;
        }

        // Calculate Derived Stats
        double totalDistance = stats.getTotalDistance();
        long totalTime = stats.getTotalTimeSeconds();
        double avgSpeedKmh = (totalTime > 0) ? (totalDistance / totalTime) * KMH_CONVERSION_FACTOR : 0;

        // Get Rank Strings
        String distanceRankStr = (ranks.distanceRank > 0) ? ColorHelper.parse(" &#FFD700(#" + ranks.distanceRank + ")") : "";
        String timeRankStr = (ranks.timeRank > 0) ? ColorHelper.parse(" &#FFD700(#" + ranks.timeRank + ")") : "";
        String longestFlightRankStr = (ranks.longestFlightRank > 0) ? ColorHelper.parse(" &#FFD700(#" + ranks.longestFlightRank + ")") : "";

        int effectsOwned = 0;
        int totalEffects = effectsHandler.getEffectsRegistry().size();
        String activeEffect = "None";
        try {
            if (sender instanceof Player player){
                if (PermissionsHelper.hasAllEffectsPermission(player)){
                    effectsOwned = totalEffects;
                }else {
                    effectsOwned = databaseHandler.getOwnedEffectKeys(stats.getUuid()).size();
                }
            }

            String storedEffect = databaseHandler.getPlayerActiveEffect(stats.getUuid());
            if (storedEffect != null) activeEffect = storedEffect;
        } catch (SQLException e) {
            messagesHelper.sendCommandSenderMessage(sender,"&cCould not load effect data...");
            logger.log(Level.SEVERE, "Could not load effect data: ", e);
        }

        // Formatting and Sending Message
        var primary = "§6";
        var secondary = "§e";
        var text = "§7";
        var value = "§f";
        var arrow = "» ";

        sender.sendMessage(primary + "§m----------------------------------------------------");
        sender.sendMessage("");
        sender.sendMessage(primary + "§l" + titlePrefix + " Elytra Statistics");
        sender.sendMessage("");

        sender.sendMessage(secondary + arrow + text + "Total Distance Flown: " + value + String.format("%.1f km", totalDistance / METERS_IN_ONE_KILOMETER) + distanceRankStr);
        sender.sendMessage(secondary + arrow + text + "Total Flight Time: " + value + TimeHelper.formatFlightTime((int) totalTime) + timeRankStr);
        sender.sendMessage(secondary + arrow + text + "Longest Flight: " + value + String.format("%.0f blocks", stats.getLongestFlight()) + longestFlightRankStr);
        sender.sendMessage(secondary + arrow + text + "Average Speed: " + value + String.format("%.1f km/h", avgSpeedKmh));
        sender.sendMessage("");
        sender.sendMessage(secondary + arrow + text + "Boosts Used: " + value + String.format("%d (%d Super Boosts)", stats.getBoostsUsed(), stats.getSuperBoostsUsed()));
        sender.sendMessage(secondary + arrow + text + "Saves: " + value + String.format("%d times", stats.getPluginSaves()));
        sender.sendMessage(secondary + arrow + text + "Effects Unlocked: " + value + String.format("%d/%d", effectsOwned, totalEffects));
        sender.sendMessage(secondary + arrow + text + "Active Effect: " + value + activeEffect);

        sender.sendMessage("");
        sender.sendMessage(primary + "§m----------------------------------------------------");
    }
}
