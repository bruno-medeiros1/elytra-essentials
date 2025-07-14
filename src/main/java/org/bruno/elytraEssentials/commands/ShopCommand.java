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
            plugin.getMessagesHelper().sendCommandSenderMessage(sender,"&cOnly players can use this command.");
            return true;
        }

        if (!PermissionsHelper.hasShopPermission(player)){
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return true;
        }

        // The command always opens the first page (page 0)
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);
        openShop(player, 0);
        return true;
    }

    public void openShop(Player player, int page) {
        plugin.getShopGuiListener().setCurrentPage(player.getUniqueId(), page);

        Inventory shop = Bukkit.createInventory(new ShopHolder(), Constants.GUI.SHOP_INVENTORY_SIZE, Constants.GUI.SHOP_INVENTORY_NAME);

        populateShopItems(shop, player, page);
        addControlButtons(shop, player, page);

        player.openInventory(shop);
    }

    private void populateShopItems(Inventory shop, Player player, int page) {
        List<Map.Entry<String, ElytraEffect>> effectsList = new ArrayList<>(plugin.getEffectsHandler().getEffectsRegistry().entrySet());

        if (effectsList.isEmpty()) {
            plugin.getLogger().warning("There are no effects created. Shop will not work");
            return;
        }

        // Fill the border with panes
        ItemStack fillerPane = GuiHelper.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < Constants.GUI.SHOP_INVENTORY_SIZE; i++) {
            boolean isItemSlot = (i >= 10 && i <= 16) || (i >= 19 && i <= 25);
            boolean isControlSlot = (i >= 36);
            if (!isItemSlot && !isControlSlot) {
                shop.setItem(i, fillerPane);
            }
        }

        int startIndex = page * Constants.GUI.SHOP_ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + Constants.GUI.SHOP_ITEMS_PER_PAGE, effectsList.size());

        int guiSlotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, ElytraEffect> entry = effectsList.get(i);

            // If effect is not supported in this version, skip it
            if (entry.getValue() == null) {
                continue;
            }

            ItemStack item = plugin.getEffectsHandler().createShopItem(entry.getKey(), entry.getValue(), player);

            if (guiSlotIndex < Constants.GUI.SHOP_ITEM_SLOTS.size()) {
                shop.setItem(Constants.GUI.SHOP_ITEM_SLOTS.get(guiSlotIndex), item);
            } else {
                plugin.getLogger().warning("Not enough GUI slots to display all effects.");
                break;
            }

            guiSlotIndex++;
        }
    }

    private void addControlButtons(Inventory shop, Player player, int page) {
        int totalItems = plugin.getEffectsHandler().getEffectsRegistry().size();
        int totalPages = (totalItems == 0) ? 1 : (int) Math.ceil((double) totalItems / Constants.GUI.SHOP_ITEMS_PER_PAGE);

        shop.setItem(Constants.GUI.SHOP_PLAYER_HEAD_SLOT, GuiHelper.createPlayerHead(player, "§aYour Effects", "§7Click to view your collection."));
        shop.setItem(Constants.GUI.SHOP_PREVIOUS_PAGE_SLOT, GuiHelper.createPreviousPageButton(page > 0));
        shop.setItem(Constants.GUI.SHOP_PAGE_INFO_SLOT, GuiHelper.createPageInfoItem(page + 1, totalPages));
        shop.setItem(Constants.GUI.SHOP_NEXT_PAGE_SLOT, GuiHelper.createNextPageButton(page < totalPages - 1));
        shop.setItem(Constants.GUI.SHOP_CLOSE_SLOT, GuiHelper.createCloseButton());
    }
}