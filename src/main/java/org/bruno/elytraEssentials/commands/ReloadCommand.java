package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ReloadCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    private final MessagesHelper messagesHelper;
    private final StatsHandler statsHandler;
    private final DatabaseHandler databaseHandler;
    private final MessagesHandler messagesHandler;
    private final Logger logger;

    public ReloadCommand(ElytraEssentials plugin, MessagesHelper messagesHelper, StatsHandler statsHandler, DatabaseHandler databaseHandler,
                         MessagesHandler messagesHandler, Logger logger) {
        this.plugin = plugin;
        this.messagesHelper = messagesHelper;
        this.statsHandler = statsHandler;
        this.databaseHandler = databaseHandler;
        this.messagesHandler = messagesHandler;
        this.logger = logger;
    }

    //  TODO: Review reload
    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasReloadPermission(sender)) {
            this.messagesHelper.sendCommandSenderMessage(sender, ColorHelper.parse(messagesHandler.getNoPermissionMessage()));
            return true;
        }

        this.messagesHelper.sendCommandSenderMessage(sender, ColorHelper.parse(messagesHandler.getReloadStartMessage()));

        // Start the Reload Process
        try {
            plugin.shutdownAllPluginTasks();
            databaseHandler.Disconnect();

            plugin.reloadConfig();

            //plugin.();

            // Validate the flight time for online players.
            logger.info("Reloading data for all online players...");
            //plugin.getElytraFlightListener().reloadOnlinePlayerFlightTimes();

            // Re-assign the config variables for the ElytraFlightListener
            //plugin.getElytraFlightListener().assignConfigVariables();

            plugin.startAllPluginTasks();

            // Reload the main stats for all online players.
            for (Player player : Bukkit.getOnlinePlayers()) {
                statsHandler.loadPlayerStats(player);
            }

            this.messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getReloadSuccessMessage());
        } catch (Exception e) {
            this.messagesHelper.sendCommandSenderMessage(sender, "&cAn error occurred during reload. The server may be in an unstable state. Please check the console.");
            logger.log(Level.SEVERE, "A critical error occurred during plugin reload.", e);
        }

        return true;
    }
}