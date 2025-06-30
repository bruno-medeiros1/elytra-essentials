package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.gui.ShopHolder;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopCommand implements ISubCommand {
    private final ElytraEssentials plugin;

    public ShopCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!PermissionsHelper.hasShopPermission(player)){
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return true;
        }

        // The command always opens the first page (page 0)
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);
        OpenShop(player, 0);
        return true;
    }

    public void OpenShop(Player player, int page) {
        plugin.getShopGuiListener().setCurrentPage(player.getUniqueId(), page);

        Inventory shop = Bukkit.createInventory(new ShopHolder(), Constants.GUI.SHOP_INVENTORY_SIZE, Constants.GUI.SHOP_INVENTORY_NAME);

        populateShopItems(shop, player, page);
        addControlButtons(shop, player, page);

        player.openInventory(shop);
    }

    private void populateShopItems(Inventory shop, Player player, int page) {
        // Convert map to a list to easily get items by index for pagination
        List<Map.Entry<String, ElytraEffect>> effectsList = new ArrayList<>(plugin.getEffectsHandler().getEffectsRegistry().entrySet());

        if (effectsList.isEmpty()) {
            plugin.getLogger().warning("There are no effects created. Shop will not work");
            return;
        }

        // Fill top border and bottom non-interactive slots with panes
        ItemStack fillerPane = GuiHelper.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) shop.setItem(i, fillerPane);

        // Calculate the start and end index for the items on the current page
        int startIndex = page * Constants.GUI.SHOP_ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + Constants.GUI.SHOP_ITEMS_PER_PAGE, effectsList.size());

        int currentSlot = 10; // Start placing items after the top border
        for (int i = startIndex; i < endIndex; i++) {
            // The shop has 2 rows for items (14 slots total)
            if (currentSlot >= 26) break;

            Map.Entry<String, ElytraEffect> entry = effectsList.get(i);
            ItemStack item = plugin.getEffectsHandler().createShopItem(entry.getKey(), entry.getValue(), player);

            // Logic to correctly place items in two rows
            int slot = (currentSlot < 17) ? currentSlot : currentSlot + 2; // Skip row divider
            if (slot > 25) break; // Ensure we don't go out of bounds

            shop.setItem(slot, item);
            currentSlot++;
        }
    }

    private void addControlButtons(Inventory shop, Player player, int page) {
        int totalItems = plugin.getEffectsHandler().getEffectsRegistry().size();
        // Calculate total pages, ensuring at least 1 page even if empty
        int totalPages = (totalItems == 0) ? 1 : (int) Math.ceil((double) totalItems / Constants.GUI.SHOP_ITEMS_PER_PAGE);

        // Player Head
        shop.setItem(Constants.GUI.SHOP_PLAYER_HEAD_SLOT, GuiHelper.createPlayerHead(player, "§aYour Effects", "§7Click to view your collection."));

        // Previous Page Button
        if (page > 0) {
            shop.setItem(Constants.GUI.SHOP_PREVIOUS_PAGE_SLOT, GuiHelper.createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aPrevious Page", String.format("§7Click to go to page %d.", page)));
        } else {
            shop.setItem(Constants.GUI.SHOP_PREVIOUS_PAGE_SLOT, GuiHelper.createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cPrevious Page", "§7You are on the first page."));
        }

        // Page Info Compass
        shop.setItem(Constants.GUI.SHOP_PAGE_INFO_SLOT, GuiHelper.createGuiItem(Material.COMPASS, String.format("§ePage %d / %d", page + 1, totalPages)));

        // Next Page Button
        if (page < totalPages - 1) {
            shop.setItem(Constants.GUI.SHOP_NEXT_PAGE_SLOT, GuiHelper.createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aNext Page", String.format("§7Click to go to page %d.", page + 2)));
        } else {
            shop.setItem(Constants.GUI.SHOP_NEXT_PAGE_SLOT, GuiHelper.createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cNext Page", "§7You are on the last page."));
        }

        // Close Menu Barrier
        shop.setItem(Constants.GUI.SHOP_CLOSE_SLOT, GuiHelper.createGuiItem(Material.BARRIER, "§cClose Menu", "§7Click to exit the shop."));
    }
}