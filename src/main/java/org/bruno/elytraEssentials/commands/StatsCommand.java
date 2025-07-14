package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class StatsCommand implements ISubCommand {
    private static final double METERS_IN_ONE_KILOMETER = 1000.0;
    private static final double KMH_CONVERSION_FACTOR = 3.6;

    private final ElytraEssentials plugin;

    // A simple record to hold the player's ranks
    private record PlayerRanks(int distanceRank, int timeRank, int longestFlightRank) {}

    public StatsCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.getMessagesHelper().sendCommandSenderMessage(sender,"&cUsage: /ee stats <player>");
                return true;
            }
            if (!PermissionsHelper.hasStatsPermission(player)) {
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
                return true;
            }
            fetchAndDisplayStats(player, player);
            return true;
        } else if (args.length == 1) {
            if (!PermissionsHelper.hasStatsOthersPermission(sender)) {
                plugin.getMessagesHelper().sendCommandSenderMessage(sender, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
                return true;
            }

            String targetName = args[0];
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                String message = plugin.getMessagesHandlerInstance().getPlayerNotFound().replace("{0}", targetName);
                plugin.getMessagesHelper().sendCommandSenderMessage(sender, message);
                return true;
            }

            fetchAndDisplayStats(sender, targetPlayer);
            return true;
        }

        plugin.getMessagesHelper().sendCommandSenderMessage(sender,"Usage: /ee stats <player>");
        return true;
    }

    private void fetchAndDisplayStats(CommandSender sender, OfflinePlayer target) {
        plugin.getMessagesHelper().sendCommandSenderMessage(sender,"&eFetching stats for " + target.getName() + "...");

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PlayerStats targetStats = plugin.getDatabaseHandler().getPlayerStats(target.getUniqueId());

                    // Fetch ranks for the leaderboard stats
                    int distanceRank = plugin.getDatabaseHandler().getPlayerRank(target.getUniqueId(), "total_distance");
                    int timeRank = plugin.getDatabaseHandler().getPlayerRank(target.getUniqueId(), "total_time_seconds");
                    int longestFlightRank = plugin.getDatabaseHandler().getPlayerRank(target.getUniqueId(), "longest_flight");
                    PlayerRanks ranks = new PlayerRanks(distanceRank, timeRank, longestFlightRank);

                    // Switch back to the main thread to display the stats
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            String titlePrefix = (sender instanceof Player p && p.getUniqueId().equals(target.getUniqueId())) ? "Your" : target.getName() + "'s";
                            displayStats(sender, targetStats, ranks, titlePrefix);
                        }
                    }.runTask(plugin);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not fetch stats for " + target.getName(), e);
                    plugin.getMessagesHelper().sendCommandSenderMessage(sender,"&cAn error occurred while fetching stats.");
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void displayStats(CommandSender sender, PlayerStats stats, PlayerRanks ranks, String titlePrefix) {
        if (stats == null) {
            plugin.getMessagesHelper().sendCommandSenderMessage(sender, "&cCould not load stats for this player.");
            return;
        }

        // --- Calculate Derived Stats ---
        double totalDistance = stats.getTotalDistance();
        long totalTime = stats.getTotalTimeSeconds();
        double avgSpeedKmh = (totalTime > 0) ? (totalDistance / totalTime) * KMH_CONVERSION_FACTOR : 0;

        // --- Get Rank Strings ---
        String distanceRankStr = (ranks.distanceRank > 0) ? ColorHelper.parse(" &#FFD700(#" + ranks.distanceRank + ")") : "";
        String timeRankStr = (ranks.timeRank > 0) ? ColorHelper.parse(" &#FFD700(#" + ranks.timeRank + ")") : "";
        String longestFlightRankStr = (ranks.longestFlightRank > 0) ? ColorHelper.parse(" &#FFD700(#" + ranks.longestFlightRank + ")") : "";

        int effectsOwned = 0;
        int totalEffects = plugin.getEffectsHandler().getEffectsRegistry().size();
        String activeEffect = "None";
        try {
            if (sender instanceof Player player){
                if (PermissionsHelper.hasAllEffectsPermission(player)){
                    effectsOwned = totalEffects;
                }else {
                    effectsOwned = plugin.getDatabaseHandler().GetOwnedEffectKeys(stats.getUuid()).size();
                }
            }

            String storedEffect = plugin.getDatabaseHandler().getPlayerActiveEffect(stats.getUuid());
            if (storedEffect != null) activeEffect = storedEffect;
        } catch (SQLException e) {
            plugin.getMessagesHelper().sendCommandSenderMessage(sender,"&cCould not load effect data...");
            plugin.getLogger().log(Level.SEVERE, "Could not load effect data: ", e);
        }

        // --- Formatting and Sending Message ---
        ChatColor primary = ChatColor.GOLD;
        ChatColor secondary = ChatColor.YELLOW;
        ChatColor text = ChatColor.GRAY;
        ChatColor value = ChatColor.WHITE;
        String arrow = "» ";

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
        // ... (your existing logic to display effects) ...
        sender.sendMessage("");
        sender.sendMessage(primary + "§m----------------------------------------------------");
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (PermissionsHelper.hasStatsOthersPermission(sender)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }
}