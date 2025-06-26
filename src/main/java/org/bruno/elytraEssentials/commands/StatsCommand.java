package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.PlayerStats;
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

        player.sendMessage("§6--- Your Elytra Statistics ---");
        player.sendMessage(String.format("§eTotal Distance Flown: §f%.1f km", totalDistance / METERS_IN_ONE_KILOMETER));
        player.sendMessage("§eTotal Flight Time: §f" + TimeHelper.formatFlightTime((int) totalTime));
        player.sendMessage(String.format("§eLongest Flight: §f%.0f blocks", stats.getLongestFlight()));
        player.sendMessage(String.format("§eAverage Speed: §f%.1f km/h", avgSpeedKmh));
        player.sendMessage(""); // Spacer
        player.sendMessage(String.format("§eBoosts Used: §f%d (%d Super Boosts)", stats.getBoostsUsed(), stats.getSuperBoostsUsed()));
        player.sendMessage(String.format("§ePlugin Saves: §f%d times", stats.getPluginSaves()));
        player.sendMessage(String.format("§eEffects Unlocked: §f%d/%d", effectsOwned, totalEffects));
        player.sendMessage("§eActive Effect: §f" + activeEffect);
        player.sendMessage("§6---------------------------");

        return true;
    }
}