package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class ReloadCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public ReloadCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasReloadPermission(sender)) {
            sender.sendMessage(ColorHelper.parse(plugin.getMessagesHandlerInstance().getNoPermissionMessage()));
            return true;
        }

        sender.sendMessage(ColorHelper.parse(plugin.getMessagesHandlerInstance().getReloadStartMessage()));

        // Start the Reload Process
        try {
            plugin.cancelAllPluginTasks();
            plugin.getDatabaseHandler().saveAllData();
            plugin.getDatabaseHandler().Disconnect();

            plugin.reloadConfig();

            plugin.setupHandlers();

            if (!plugin.setupDatabase()) {
                sender.sendMessage("§cReload failed: Could not re-establish database connection. Check console.");
                return true;
            }

            plugin.startAllPluginTasks();

            // Validate the flight time for online players.
            plugin.getMessagesHelper().sendConsoleLog("info", "Reloading data for all online players...");
            plugin.getElytraFlightListener().reloadOnlinePlayerFlightTimes();

            // Reload the main stats for all online players.
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getStatsHandler().loadPlayerStats(player);
            }

            sender.sendMessage(ColorHelper.parse(plugin.getMessagesHandlerInstance().getReloadSuccessMessage()));
        } catch (Exception e) {
            sender.sendMessage("§cAn error occurred during reload. The server may be in an unstable state. Please check the console.");
            plugin.getLogger().log(Level.SEVERE, "A critical error occurred during plugin reload.", e);
        }

        return true;
    }
}