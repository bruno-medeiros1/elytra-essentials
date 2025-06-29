package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class StatsCommand implements ISubCommand {
    private final static double METERS_IN_ONE_KILOMETER = 1000;
    private static final double KMH_CONVERSION_FACTOR = 3.6;

    private final ElytraEssentials plugin;
    private final StatsHandler statsHandler;

    public StatsCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.statsHandler = plugin.getStatsHandler();
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) throws SQLException {
        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("§cThis command can only be run by players or the console.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                return true;
            }

            if (!PermissionsHelper.hasStatsPermission(player)) {
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
                return true;
            }

            PlayerStats stats = statsHandler.getStats(player);
            displayStats(player, stats, "Your");
            return true;
        }
        else if (args.length == 1) {
            if (sender instanceof Player player){
                if (!PermissionsHelper.hasOthersStatsPermission(player)) {
                    sender.sendMessage(plugin.getMessagesHandlerInstance().getNoPermissionMessage());
                    return true;
                }
            }

            String targetName = args[0];
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                String message = plugin.getMessagesHandlerInstance().getPlayerNotFound()
                                .replace("{0}", targetName);
                sender.sendMessage(message);
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "Fetching stats for " + targetName + "...");

            // Fetch stats asynchronously to avoid lagging the server for a database lookup
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        PlayerStats targetStats = plugin.getDatabaseHandler().getPlayerStats(targetPlayer.getUniqueId());
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                displayStats(sender, targetStats, targetPlayer.getName() + "'s");
                            }
                        }.runTask(plugin);
                    } catch (SQLException e) {
                        sender.sendMessage(ChatColor.RED + "An error occurred while fetching stats.");
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(plugin);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /ee stats <player>");
        return true;
    }

    /**
     * Formats and sends the statistics message to a viewer.
     * @param viewer The CommandSender who will see the message.
     * @param stats  The PlayerStats object containing the data.
     * @param titlePrefix The prefix for the title (e.g., "Your" or "PlayerName's").
     */
    private void displayStats(CommandSender viewer, PlayerStats stats, String titlePrefix) {
        if (stats == null) {
            viewer.sendMessage(ChatColor.RED + "Could not load stats for this player.");
            return;
        }

        double totalDistance = stats.getTotalDistance();
        long totalTime = stats.getTotalTimeSeconds();
        double avgSpeedKmh = (totalTime > 0) ? (totalDistance / totalTime) * KMH_CONVERSION_FACTOR : 0;

        int effectsOwned = 0;
        int totalEffects = plugin.getEffectsHandler().getEffectsRegistry().size();
        String activeEffect = "None";
        try {
            effectsOwned = plugin.getDatabaseHandler().GetOwnedEffectKeys(stats.getUuid()).size();
            String storedEffect = plugin.getDatabaseHandler().getPlayerActiveEffect(stats.getUuid());
            if (storedEffect != null) activeEffect = storedEffect;
        } catch (SQLException e) {
            viewer.sendMessage(ChatColor.RED + "Could not load effect data.");
            e.printStackTrace();
        }

        // --- Formatting Constants ---
        ChatColor primary = ChatColor.GOLD;
        ChatColor secondary = ChatColor.YELLOW;
        ChatColor text = ChatColor.GRAY;
        ChatColor value = ChatColor.WHITE;
        String arrow = "» ";

        // --- Send Messages ---
        viewer.sendMessage(primary + "§m----------------------------------------------------");
        viewer.sendMessage("");
        viewer.sendMessage(primary + "§l" + titlePrefix + " Elytra Statistics");
        viewer.sendMessage("");

        viewer.sendMessage(secondary + arrow + text + "Total Distance Flown: " + value + String.format("%.1f km", totalDistance / METERS_IN_ONE_KILOMETER));
        viewer.sendMessage(secondary + arrow + text + "Total Flight Time: " + value + TimeHelper.formatFlightTime((int) totalTime));
        viewer.sendMessage(secondary + arrow + text + "Longest Flight: " + value + String.format("%.0f blocks", stats.getLongestFlight()));
        viewer.sendMessage(secondary + arrow + text + "Average Speed: " + value + String.format("%.1f km/h", avgSpeedKmh));
        viewer.sendMessage("");
        viewer.sendMessage(secondary + arrow + text + "Boosts Used: " + value + String.format("%d (%d Super Boosts)", stats.getBoostsUsed(), stats.getSuperBoostsUsed()));
        viewer.sendMessage(secondary + arrow + text + "Plugin Saves: " + value + String.format("%d times", stats.getPluginSaves()));
        viewer.sendMessage(secondary + arrow + text + "Effects Unlocked: " + value + String.format("%d/%d", effectsOwned, totalEffects));
        viewer.sendMessage(secondary + arrow + text + "Active Effect: " + value + activeEffect);
        viewer.sendMessage("");
        viewer.sendMessage(primary + "§m----------------------------------------------------");
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (sender instanceof Player player) {
                if (!PermissionsHelper.hasOthersStatsPermission(player)) {
                    return List.of();
                }
            }

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}