package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.gui.forge.ForgeGuiHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForgeCommand implements ISubCommand {

    private final ForgeGuiHandler forgeGuiHandler;
    private final ConfigHandler configHandler;
    private final MessagesHelper messagesHelper;

    public ForgeCommand(ForgeGuiHandler forgeGuiHandler, ConfigHandler configHandler, MessagesHelper messagesHelper) {
        this.forgeGuiHandler = forgeGuiHandler;
        this.configHandler = configHandler;
        this.messagesHelper = messagesHelper;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!configHandler.getIsArmoredElytraEnabled()){
            messagesHelper.sendPlayerMessage(player, "&cThis feature is currently disabled.");
            return true;
        }

        if (!PermissionsHelper.hasForgePermission(player)){
            messagesHelper.sendPlayerMessage(player, "&cYou do not have permission to use the forge.");
            return true;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.8f);
        forgeGuiHandler.openForge(player);
        return true;
    }
}
