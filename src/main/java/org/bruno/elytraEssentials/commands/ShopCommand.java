package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.gui.shop.ShopGuiHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements ISubCommand {
    private final ShopGuiHandler shopGuiHandler;
    private final MessagesHelper messagesHelper;

    public ShopCommand(ShopGuiHandler shopGuiHandler, MessagesHelper messagesHelper) {
        this.shopGuiHandler = shopGuiHandler;
        this.messagesHelper = messagesHelper;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messagesHelper.sendCommandSenderMessage(sender,"&cOnly players can use this command.");
            return true;
        }

        if (!PermissionsHelper.hasShopPermission(player)) {
            messagesHelper.sendCommandSenderMessage(sender, "&cYou do not have permission to use the shop.");
            return true;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);
        shopGuiHandler.openShop(player, 0);
        return true;
    }
}