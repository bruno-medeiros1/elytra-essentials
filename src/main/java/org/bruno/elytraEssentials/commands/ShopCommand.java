package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.gui.shop.ShopGuiHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements SubCommand {
    private final ShopGuiHandler shopGuiHandler;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler;

    public ShopCommand(ShopGuiHandler shopGuiHandler, MessagesHelper messagesHelper, MessagesHandler messagesHandler) {
        this.shopGuiHandler = shopGuiHandler;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messagesHelper.sendCommandSenderMessage(sender,"&cOnly players can use this command.");
            return true;
        }

        if (!PermissionsHelper.hasShopPermission(player)) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getNoPermissionMessage());
            return true;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);
        shopGuiHandler.openShop(player, 0);
        return true;
    }
}