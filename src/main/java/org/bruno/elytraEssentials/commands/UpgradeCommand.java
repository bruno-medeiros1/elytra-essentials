package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.gui.upgrade.UpgradeGuiHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.ArmoredElytraHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UpgradeCommand implements SubCommand {
    private final UpgradeGuiHandler upgradeGuiHandler;
    private final ArmoredElytraHelper armoredElytraHelper;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler;

    public UpgradeCommand(UpgradeGuiHandler upgradeGuiHandler, ArmoredElytraHelper armoredElytraHelper, MessagesHelper messagesHelper, MessagesHandler messagesHandler) {
        this.upgradeGuiHandler = upgradeGuiHandler;
        this.armoredElytraHelper = armoredElytraHelper;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messagesHelper.sendCommandSenderMessage(sender, "&cThis command can only be run by a player.");
            return true;
        }

        if (!PermissionsHelper.hasUpgradePermission(player)) {
            messagesHelper.sendPlayerMessage(player, messagesHandler.getNoPermissionMessage());
            return true;
        }

        // Check if the player is wearing an Armored Elytra.
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || !armoredElytraHelper.isArmoredElytra(chestplate)) {
            messagesHelper.sendPlayerMessage(player, "&cYou must be wearing an Armored Elytra to use the upgrade station.");
            return true;
        }

        if (player.isGliding()){
            messagesHelper.sendCommandSenderMessage(sender, "&cYou must be on the ground to upgrade your armored elytra.");
            return true;
        }

        upgradeGuiHandler.open(player);
        return true;
    }
}
