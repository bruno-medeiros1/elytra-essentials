package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
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
    public boolean Execute(CommandSender sender, String[] args) {
        MessagesHandler messagesHandler = this.plugin.getMessagesHandlerInstance();
        MessagesHelper messagesHelper = this.plugin.getMessagesHelper();

        if (sender instanceof Player) {
            if (!(sender.hasPermission("elytraEssentials.command.reload") && sender.hasPermission("elytraEssentials.command.*")
                    && sender.hasPermission("elytraEssentials.*"))) {
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

    private void ReloadPlugin() {
        try {
            Bukkit.getScheduler().cancelTasks(this.plugin);
            this.plugin.getDatabaseHandler().Disconnect();

            this.plugin.getMessagesHelper().sendConsoleMessage("&aAll background tasks disabled successfully!");
        } catch (Exception exception) {
            this.plugin.getMessagesHelper().sendConsoleMessage("&aAll background tasks disabled successfully!");
        }

        this.plugin.getMessagesHelper().sendConsoleMessage("&aReloading config.yml...");
        this.plugin.reloadConfig();

        //  We update colorHelper reference since messages.yml could have changed
        ColorHelper colorHelper = new ColorHelper(this.plugin);
        this.plugin.setColorHelper(colorHelper);

        this.plugin.getMessagesHelper().sendConsoleMessage("&aReloading messages.yml...");
        ConfigHandler configHandler = new ConfigHandler(this.plugin.getConfig());
        this.plugin.setConfigHandler(configHandler);

        MessagesHandler messagesHandler = new MessagesHandler(colorHelper.GetFileConfiguration());
        this.plugin.setMessagesHandler(messagesHandler);

        MessagesHelper messagesHelper = new MessagesHelper(this.plugin);
        this.plugin.SetMessagesHelper(messagesHelper);

        this.plugin.getElytraFlightListener().AssignConfigVariables();

        //  handle database on reload
        var database = this.plugin.getDatabaseHandler();
        database.SetDatabaseVariables();
        try {
            database.Initialize();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        this.plugin.getMessagesHelper().SetDebugMode(this.plugin.getConfigHandlerInstance().getIsDebugModeEnabled());
    }
}
