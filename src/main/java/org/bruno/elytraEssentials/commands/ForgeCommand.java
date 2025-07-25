package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.gui.forge.ForgeGuiHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForgeCommand implements SubCommand {

    private final ForgeGuiHandler forgeGuiHandler;
    private final ConfigHandler configHandler;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler;

    public ForgeCommand(ForgeGuiHandler forgeGuiHandler, ConfigHandler configHandler, MessagesHelper messagesHelper, MessagesHandler messagesHandler) {
        this.forgeGuiHandler = forgeGuiHandler;
        this.configHandler = configHandler;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!configHandler.getIsArmoredElytraEnabled()){
            messagesHelper.sendPlayerMessage(player, messagesHandler.getFeatureNotEnabled());
            return true;
        }

        if (!PermissionsHelper.hasForgePermission(player)){
            messagesHelper.sendPlayerMessage(player, messagesHandler.getNoPermissionMessage());
            return true;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.8f);
        forgeGuiHandler.openForge(player);
        return true;
    }
}
