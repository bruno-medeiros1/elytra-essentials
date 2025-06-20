package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.handlers.EffectsHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.handlers.RecoveryHandler;
import org.bruno.elytraEssentials.helpers.ColorHelper;
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
        }
        else if (sender instanceof ConsoleCommandSender) {
            messagesHelper.sendConsoleMessage(messagesHandler.getReloadStartMessage());
            this.ReloadPlugin();
            messagesHelper.sendConsoleMessage(messagesHandler.getReloadSuccessMessage());
        }
        return true;
    }

    private void ReloadPlugin() throws SQLException {
        try {
            Bukkit.getScheduler().cancelTasks(this.plugin);
            this.plugin.getDatabaseHandler().Disconnect();

            this.plugin.getMessagesHelper().sendConsoleMessage("&aAll background tasks disabled successfully!");
        } catch (Exception exception) {
            this.plugin.getMessagesHelper().sendConsoleMessage("&aAll background tasks disabled successfully!");
        }

        boolean isOldTimeLimitEnabled = plugin.getConfigHandlerInstance().getIsTimeLimitEnabled();

        this.plugin.getMessagesHelper().sendConsoleMessage("&aReloading config.yml...");
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

        //  handle database on reload
        var database = this.plugin.getDatabaseHandler();
        database.SetDatabaseVariables();
        database.Initialize();

        //  ElytraFlightListener
        this.plugin.getElytraFlightListener().AssignConfigVariables();

        //  Handling flight time data structures to be updated in the listener
        boolean isNewTimeLimitEnabled = plugin.getConfigHandlerInstance().getIsTimeLimitEnabled();
        if (!isOldTimeLimitEnabled && isNewTimeLimitEnabled)
            this.plugin.getElytraFlightListener().validateFlightTimeOnReload();


        this.plugin.getMessagesHelper().setDebugMode(this.plugin.getConfigHandlerInstance().getIsDebugModeEnabled());
    }
}
