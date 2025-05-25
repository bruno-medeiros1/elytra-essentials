package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements CommandExecutor {

    private final ElytraEssentials elytraEssentials;

    public ReloadCommand(ElytraEssentials elytraEssentials) {
        this.elytraEssentials = elytraEssentials;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        MessagesHandler messagesHandler = this.elytraEssentials.getMessagesHandlerInstance();

        if (commandSender instanceof Player) {
            if (!(commandSender.hasPermission("elytraEssentials.command.reload") && commandSender.hasPermission("elytraEssentials.command.*")
                    && commandSender.hasPermission("elytraEssentials.*"))) {
                MessagesHelper.sendPlayerMessage((Player)commandSender, messagesHandler.getNoPermissionMessage());
                return true;
            }
            MessagesHelper.sendPlayerMessage((Player)commandSender, messagesHandler.getReloadBeginMessage());
            this.ReloadPlugin();
        }
        else if (commandSender instanceof ConsoleCommandSender) {
            MessagesHelper.sendConsoleMessage(messagesHandler.getReloadBeginMessage());
            this.ReloadPlugin();
            MessagesHelper.sendConsoleMessage(messagesHandler.getReloadSuccessMessage());
        }
        return true;
    }

    private void ReloadPlugin() {
        try {
            Bukkit.getScheduler().cancelTasks(this.elytraEssentials);
            MessagesHelper.sendConsoleMessage("&aAll background tasks disabled successfully!");
        } catch (Exception exception) {
            MessagesHelper.sendConsoleMessage("&aAll background tasks disabled successfully!");
        }

        MessagesHelper.sendConsoleMessage("&aReloading config.yml...");
        this.elytraEssentials.reloadConfig();

        //  We update colorHelper reference since messages.yml could have changed
        ColorHelper colorHelper = new ColorHelper(this.elytraEssentials);
        this.elytraEssentials.setColorHelper(colorHelper);

        MessagesHelper.sendConsoleMessage("&aReloading messages.yml...");
        ConfigHandler configHandler = new ConfigHandler(this.elytraEssentials.getConfig());
        this.elytraEssentials.setConfigHandler(configHandler);


        MessagesHandler messagesHandler = new MessagesHandler(colorHelper.GetFileConfiguration());
        this.elytraEssentials.setMessagesHandler(messagesHandler);

        this.elytraEssentials.getElytraFlightListener().AssignConfigVariables();

        MessagesHelper.SetDebugMode(this.elytraEssentials.getConfigHandlerInstance().getDeveloperModeIsEnabled());

    }
}
