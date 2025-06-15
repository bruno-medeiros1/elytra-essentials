package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.gui.ShopHolder;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ShopCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public ShopCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        MessagesHandler messagesHandler = this.plugin.getMessagesHandlerInstance();
        MessagesHelper messagesHelper = this.plugin.getMessagesHelper();

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        boolean canOpen = PlayerCanOpenShop((Player) sender);
        if (!canOpen) {
            messagesHelper.sendPlayerMessage((Player) sender, messagesHandler.getNoPermissionMessage());
            return true;
        }

        OpenShop((Player) sender);
        return true;
    }

    private void OpenShop(Player player) {
        Inventory shop = Bukkit.createInventory(new ShopHolder(), 27, ChatColor.GOLD + "§lEffects Shop");

        Map<String, ElytraEffect> effects = plugin.getEffectsHandler().getEffectsRegistry();
        if (effects.isEmpty()) {
            plugin.getLogger().warning("There are no effects created. Shop will not work");
            return;
        }

        int index = 0; // Start index for placing items in the shop
        for (Map.Entry<String, ElytraEffect> entry : effects.entrySet()) { // Iterate through map entries
            String key = entry.getKey();
            ElytraEffect elytraEffect = entry.getValue();

            if (elytraEffect == null)
                continue;

            // Pass both the effect and the key to createShopItem
            ItemStack item = plugin.getEffectsHandler().createShopItem(key, elytraEffect, player);
            shop.setItem(index, item); // Place the item in the shop
            index++; // Increment the index for the next item
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        player.openInventory(shop);
    }

    private boolean PlayerCanOpenShop(Player player) {
        return player.hasPermission("elytraEssentials.*") ||
                player.hasPermission("elytraEssentials.command.*") ||
                player.hasPermission("elytraEssentials.command.shop");
    }
}