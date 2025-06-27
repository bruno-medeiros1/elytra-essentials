package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.*;
import org.bruno.elytraEssentials.helpers.FileHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class ReloadCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public ReloadCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) throws SQLException {
        MessagesHandler messagesHandler = this.plugin.getMessagesHandlerInstance();
        MessagesHelper messagesHelper = this.plugin.getMessagesHelper();

        if (sender instanceof Player) {
            if (!(PermissionsHelper.hasReloadPermission((Player) sender))) {
                messagesHelper.sendPlayerMessage((Player)sender, messagesHandler.getNoPermissionMessage());
                return true;
            }
            messagesHelper.sendPlayerMessage((Player)sender, messagesHandler.getReloadStartMessage());
            this.ReloadPlugin();
            messagesHelper.sendPlayerMessage((Player)sender, messagesHandler.getReloadSuccessMessage());
        }
        else if (sender instanceof ConsoleCommandSender) {
            this.ReloadPlugin();
        }
        return true;
    }

    private void ReloadPlugin() throws SQLException {
        try {
            this.plugin.getMessagesHelper().sendConsoleMessage("&e###########################################");
            this.plugin.getMessagesHelper().sendConsoleMessage(plugin.getMessagesHandlerInstance().getReloadStartMessage());

            this.plugin.getMessagesHelper().sendConsoleMessage("&aShutting down background tasks...");
            Bukkit.getScheduler().cancelTasks(this.plugin);

            this.plugin.getMessagesHelper().sendConsoleMessage("&aSaving to database...");
            this.plugin.getDatabaseHandler().save();

            this.plugin.getMessagesHelper().sendConsoleMessage("&aDisconnecting the database...");
            this.plugin.getDatabaseHandler().cancelBackupTask();
            this.plugin.getDatabaseHandler().Disconnect();

        } catch (Exception exception) {
            this.plugin.getMessagesHelper().sendConsoleMessage("&aAll background tasks disabled successfully!");
        }

        this.plugin.getMessagesHelper().sendConsoleMessage("&aReloading config.yml...");
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();

        FileHelper fileHelper = new FileHelper(plugin);
        plugin.setFileHelper(fileHelper);

        this.plugin.getMessagesHelper().sendConsoleMessage("&aReloading messages.yml...");
        ConfigHandler configHandler = new ConfigHandler(this.plugin.getConfig());
        this.plugin.setConfigHandler(configHandler);

        MessagesHandler messagesHandler = new MessagesHandler(fileHelper.GetMessagesFileConfiguration());
        this.plugin.setMessagesHandler(messagesHandler);

        MessagesHelper messagesHelper = new MessagesHelper(this.plugin);
        this.plugin.SetMessagesHelper(messagesHelper);

        this.plugin.getMessagesHelper().sendConsoleMessage("&aReloading shop.yml...");
        EffectsHandler effectsHandler = new EffectsHandler(this.plugin, fileHelper.GetShopFileConfiguration());
        this.plugin.setEffectsHandler(effectsHandler);

        RecoveryHandler recoveryHandler = new RecoveryHandler(this.plugin);
        this.plugin.setRecoveryHandler(recoveryHandler);

        StatsHandler statsHandler = new StatsHandler(this.plugin);
        this.plugin.setStatsHandler(statsHandler);

        //  ElytraFlightListener
        this.plugin.getElytraFlightListener().AssignConfigVariables();

        this.plugin.getMessagesHelper().setDebugMode(this.plugin.getConfigHandlerInstance().getIsDebugModeEnabled());

        this.plugin.getMessagesHelper().sendConsoleMessage("&aRe-connecting to database and starting tasks...");
        var database = this.plugin.getDatabaseHandler();
        database.setDatabaseVariables();
        database.Initialize();
        database.startAutoBackupTask();

        this.plugin.registerPlaceholders();

        this.plugin.getMessagesHelper().sendConsoleMessage("&aReloading stats for all online players...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.plugin.getStatsHandler().loadPlayerStats(player);
        }

        this.plugin.getTpsHandler().start();
        this.plugin.getStatsHandler().start();
        this.plugin.getRecoveryHandler().start();

        messagesHelper.sendConsoleMessage(messagesHandler.getReloadSuccessMessage());
        messagesHelper.sendConsoleMessage("&e###########################################");
    }
}
