package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class StatsCommand implements ISubCommand {
    private final static double METERS_IN_ONE_KILOMETER = 1000;

    private final ElytraEssentials plugin;
    private final StatsHandler statsHandler;

    public StatsCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.statsHandler = plugin.getStatsHandler();
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) throws SQLException {
        if (!(sender instanceof Player player))
            return true;

        if (!PermissionsHelper.hasStatsPermission(player)){
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return true;
        }

        // Get the cached stats object for the player
        PlayerStats stats = statsHandler.getStats(player);

        // Average Speed
        double totalDistance = stats.getTotalDistance();
        long totalTime = stats.getTotalTimeSeconds();
        double avgSpeedKmh = 0;
        if (totalTime > 0) {
            double avgSpeedBps = totalDistance / totalTime;
            avgSpeedKmh = avgSpeedBps * 3.6;
        }

        int effectsOwned = plugin.getDatabaseHandler().GetOwnedEffectKeys(player.getUniqueId()).size();
        int totalEffects = plugin.getEffectsHandler().getEffectsRegistry().size();
        String activeEffect = plugin.getDatabaseHandler().getPlayerActiveEffect(player.getUniqueId());
        if (activeEffect == null)
            activeEffect = "None";

        ChatColor primary = ChatColor.GOLD;
        ChatColor secondary = ChatColor.YELLOW;
        ChatColor text = ChatColor.GRAY;
        ChatColor value = ChatColor.WHITE;
        String arrow = "» ";

        player.sendMessage(primary + "§m----------------------------------------------------");
        player.sendMessage("");
        player.sendMessage(primary + "§lYour Elytra Statistics");
        player.sendMessage("");

        // Flight Stats
        player.sendMessage(secondary + arrow + text + "Total Distance Flown: " + value + String.format("%.1f km", totalDistance / METERS_IN_ONE_KILOMETER));
        player.sendMessage(secondary + arrow + text + "Total Flight Time: " + value + TimeHelper.formatFlightTime((int) totalTime));
        player.sendMessage(secondary + arrow + text + "Longest Flight: " + value + String.format("%.0f blocks", stats.getLongestFlight()));
        player.sendMessage(secondary + arrow + text + "Average Speed: " + value + String.format("%.1f km/h", avgSpeedKmh));

        player.sendMessage("");

        // Plugin-specific Stats
        player.sendMessage(secondary + arrow + text + "Boosts Used: " + value + String.format("%d (%d Super Boosts)", stats.getBoostsUsed(), stats.getSuperBoostsUsed()));
        player.sendMessage(secondary + arrow + text + "Elytra Saves: " + value + String.format("%d times", stats.getPluginSaves()));
        player.sendMessage(secondary + arrow + text + "Effects Unlocked: " + value + String.format("%d/%d", effectsOwned, totalEffects));
        player.sendMessage(secondary + arrow + text + "Active Effect: " + value + activeEffect);

        player.sendMessage("");
        player.sendMessage(primary + "§m----------------------------------------------------");
        return true;
    }
}