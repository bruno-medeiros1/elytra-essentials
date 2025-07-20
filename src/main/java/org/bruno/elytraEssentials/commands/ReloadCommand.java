package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

public class ReloadCommand implements ISubCommand {

    private final ElytraEssentials plugin;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler;

    public ReloadCommand(ElytraEssentials plugin, MessagesHelper messagesHelper, MessagesHandler messagesHandler) {
        this.plugin = plugin;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasReloadPermission(sender)) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getNoPermissionMessage());
            return true;
        }

        messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getReloadStartMessage());

        try {
            // Delegate the entire reload process to the main class
            plugin.reload();
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getReloadSuccessMessage());
        } catch (Exception e) {
            messagesHelper.sendCommandSenderMessage(sender, "&cAn error occurred during reload. Please check the console for details.");
            plugin.getLogger().log(Level.SEVERE, "A critical error occurred during plugin reload.", e);
        }

        return true;
    }
}