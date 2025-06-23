package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.constants.GuiConstants;
import org.bruno.elytraEssentials.gui.ShopHolder;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
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
        if (!(sender instanceof Player player))
            return true;

        MessagesHandler messagesHandler = this.plugin.getMessagesHandlerInstance();
        MessagesHelper messagesHelper = this.plugin.getMessagesHelper();

        boolean canOpen = PermissionsHelper.hasShopPermission(player);
        if (!canOpen) {
            messagesHelper.sendPlayerMessage(player, messagesHandler.getNoPermissionMessage());
            return true;
        }

        OpenShop(player);
        return true;
    }

    public void OpenShop(Player player) {
        Inventory shop = Bukkit.createInventory(new ShopHolder(), GuiConstants.SHOP_INVENTORY_SIZE, GuiConstants.SHOP_INVENTORY_NAME);

        // Populate the shop with items, borders, and controls
        populateShopItems(shop, player);
        addControlButtons(shop, player);

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);
        player.openInventory(shop);
    }

    //  TODO: This current implementation does not support pagination.
    private void populateShopItems(Inventory shop, Player player) {
        Map<String, ElytraEffect> effects = plugin.getEffectsHandler().getEffectsRegistry();
        if (effects.isEmpty()) {
            plugin.getLogger().warning("There are no effects created. Shop will not work");
            return;
        }

        // Use a border of glass panes for a clean look
        ItemStack fillerPane = GuiHelper.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 10; i++) shop.setItem(i, fillerPane); // Top row + first slot
        for (int i = 17; i < 27; i++) shop.setItem(i, fillerPane); // Third row + start of bottom row

        int currentSlot = 10; // Start placing items after the top border
        for (Map.Entry<String, ElytraEffect> entry : effects.entrySet()) {
            if (currentSlot >= 17) break; // Stop if we run out of space for items

            ItemStack item = plugin.getEffectsHandler().createShopItem(entry.getKey(), entry.getValue(), player);
            shop.setItem(currentSlot, item);
            currentSlot++;
        }
    }

    private void addControlButtons(Inventory shop, Player player) {
        shop.setItem(GuiConstants.SHOP_PLAYER_HEAD_SLOT, GuiHelper.createPlayerHead(player, "§bYour Effects", "§7Click to view the effects you own."));
        shop.setItem(GuiConstants.SHOP_PREVIOUS_PAGE_SLOT, GuiHelper.createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cPrevious Page", "§7You are on the first page."));
        shop.setItem(GuiConstants.SHOP_PAGE_INFO_SLOT, GuiHelper.createGuiItem(Material.COMPASS, "§ePage 1/1", "§7More effects coming soon!"));
        shop.setItem(GuiConstants.SHOP_NEXT_PAGE_SLOT, GuiHelper.createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§aNext Page", "§7You are on the last page."));
        shop.setItem(GuiConstants.SHOP_CLOSE_SLOT, GuiHelper.createGuiItem(Material.BARRIER, "§cClose Menu", "§7Click to exit the shop."));
    }
}