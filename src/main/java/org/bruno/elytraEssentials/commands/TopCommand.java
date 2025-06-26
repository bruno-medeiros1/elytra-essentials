package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TopCommand implements ISubCommand {
    private final ElytraEssentials plugin;
    private static final int LEADERBOARD_LIMIT = 5;

    public TopCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            //  TODO: Add message "only players are allowed to execute this"
            return true;

        if (!PermissionsHelper.hasTopPermission(player)){
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /ee top <distance,time,longest>");
            return true;
        }

        String category = args[0].toLowerCase();
        String dbColumn;
        String title;
        String format;

        // Determine which statistic to query based on the argument
        switch (category) {
            case "distance":
                dbColumn = "total_distance";
                title = "Top Distance Flown";
                format = "§e#%d §f%s §7- §b%.1f km";
                break;
            case "time":
                dbColumn = "total_time_seconds";
                title = "Top Flight Time";
                format = "§e#%d §f%s §7- §b%s";
                break;
            case "longest":
                dbColumn = "longest_flight";
                title = "Longest Single Flights";
                format = "§e#%d §f%s §7- §b%.0f blocks";
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid category. Use: distance, time, longestflight");
                return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Fetching leaderboard data...");

        // Run the database query asynchronously to prevent server lag
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Map<UUID, Double> topData = plugin.getDatabaseHandler().getTopStats(dbColumn, LEADERBOARD_LIMIT);
                    List<String> formattedMessages = new ArrayList<>();

                    if (topData.isEmpty()) {
                        formattedMessages.add(ChatColor.GRAY + "No statistics available for this category yet.");
                    } else {
                        int rank = 1;
                        for (Map.Entry<UUID, Double> entry : topData.entrySet()) {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                            String playerName = (player.getName() != null) ? player.getName() : "Unknown";
                            Double value = entry.getValue();

                            String formattedLine;
                            if (category.equals("time")) {
                                formattedLine = String.format(format, rank, playerName, TimeHelper.formatFlightTime(value.intValue()));
                            } else if (category.equals("distance")) {
                                formattedLine = String.format(format, rank, playerName, value / 1000.0);
                            } else {
                                formattedLine = String.format(format, rank, playerName, value);
                            }
                            formattedMessages.add(formattedLine);
                            rank++;
                        }
                    }

                    // Send the formatted messages back to the player on the main server thread
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage("§6--- " + title + " ---");
                            for (String message : formattedMessages) {
                                sender.sendMessage(message);
                            }
                            sender.sendMessage("§6-------------------------------------");
                        }
                    }.runTask(plugin);

                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "An error occurred while fetching the leaderboard.");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return List.of();

        if (args.length == 2) {
            if (!PermissionsHelper.hasTopPermission((Player) sender))
                return List.of();

            // A list of all possible leaderboard categories
            List<String> allCategories = List.of("distance", "time", "longest");
            List<String> allowedCategories = new ArrayList<>();

            // Check permission for each category before adding it as a suggestion
            allowedCategories.addAll(allCategories);

            // Now, filter the list of *allowed* categories based on what the player is typing
            String currentArg = args[1].toLowerCase();
            return allowedCategories.stream()
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
