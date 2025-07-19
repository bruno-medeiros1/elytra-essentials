package org.bruno.elytraEssentials.gui.shop;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.EffectsHandler;
import org.bruno.elytraEssentials.gui.effects.EffectsGuiHandler;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopGuiHandler {
    private final ElytraEssentials plugin;
    private final EffectsHandler effectsHandler;
    private final EffectsGuiHandler effectsGuiHandler;

    // Use a ConcurrentHashMap for thread safety, essential on Folia
    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();

    public ShopGuiHandler(ElytraEssentials plugin, EffectsHandler effectsHandler, EffectsGuiHandler effectsGuiHandler) {
        this.plugin = plugin;
        this.effectsHandler = effectsHandler;
        this.effectsGuiHandler = effectsGuiHandler;
    }

    /**
     * Creates and opens the main shop GUI for a player at a specific page.
     * This method now contains all the building logic from the old ShopCommand.
     */
    public void openShop(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);

        Inventory shop = Bukkit.createInventory(new ShopHolder(), Constants.GUI.SHOP_INVENTORY_SIZE, Constants.GUI.SHOP_INVENTORY_NAME);

        populateShopItems(shop, player, page);
        addControlButtons(shop, player, page);

        player.openInventory(shop);
    }

    private void populateShopItems(Inventory shop, Player player, int page) {
        List<Map.Entry<String, ElytraEffect>> effectsList = new ArrayList<>(effectsHandler.getEffectsRegistry().entrySet());

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
            if (entry.getValue() == null) continue;

            ItemStack item = effectsHandler.createShopItem(entry.getKey(), entry.getValue(), player);

            if (guiSlotIndex < Constants.GUI.SHOP_ITEM_SLOTS.size()) {
                shop.setItem(Constants.GUI.SHOP_ITEM_SLOTS.get(guiSlotIndex), item);
            } else {
                break;
            }
            guiSlotIndex++;
        }
    }

    private void addControlButtons(Inventory shop, Player player, int page) {
        int totalItems = effectsHandler.getEffectsRegistry().size();
        int totalPages = (totalItems == 0) ? 1 : (int) Math.ceil((double) totalItems / Constants.GUI.SHOP_ITEMS_PER_PAGE);

        shop.setItem(Constants.GUI.SHOP_PLAYER_HEAD_SLOT, GuiHelper.createPlayerHead(player, "§aYour Effects", "§7Click to view your collection."));
        shop.setItem(Constants.GUI.SHOP_PREVIOUS_PAGE_SLOT, GuiHelper.createPreviousPageButton(page > 0));
        shop.setItem(Constants.GUI.SHOP_PAGE_INFO_SLOT, GuiHelper.createPageInfoItem(page + 1, totalPages));
        shop.setItem(Constants.GUI.SHOP_NEXT_PAGE_SLOT, GuiHelper.createNextPageButton(page < totalPages - 1));
        shop.setItem(Constants.GUI.SHOP_CLOSE_SLOT, GuiHelper.createCloseButton());
    }

    /**
     * Handles all click events routed from the GlobalGuiListener.
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
        int clickedSlot = event.getSlot();

        switch (clickedSlot) {
            case Constants.GUI.SHOP_PREVIOUS_PAGE_SLOT -> handlePreviousPage(player, currentPage);
            case Constants.GUI.SHOP_NEXT_PAGE_SLOT -> handleNextPage(player, currentPage);
            case Constants.GUI.SHOP_CLOSE_SLOT -> {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 0.8f);
            }
            case Constants.GUI.SHOP_PLAYER_HEAD_SLOT -> {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 0.8f, 0.8f);
                effectsGuiHandler.open(player);
            }
            default -> handlePurchaseClick(player, clickedItem, currentPage);
        }
    }

    private void handlePreviousPage(Player player, int currentPage) {
        if (currentPage > 0) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            openShop(player, currentPage - 1);
        }
    }

    private void handleNextPage(Player player, int currentPage) {
        int totalItems = effectsHandler.getEffectsRegistry().size();
        int totalPages = (int) Math.ceil((double) totalItems / Constants.GUI.SHOP_ITEMS_PER_PAGE);
        if (currentPage < totalPages - 1) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            openShop(player, currentPage + 1);
        }
    }

    private void handlePurchaseClick(Player player, ItemStack clickedItem, int currentPage) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        String effectKey = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, Constants.NBT.EFFECT_KEY),
                PersistentDataType.STRING
        );
        if (effectKey == null) return;

        String permission = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, Constants.NBT.EFFECT_PERMISSION_KEY),
                PersistentDataType.STRING
        );

        boolean purchaseSuccess = plugin.getEffectsHandler().handlePurchase(player, effectKey, permission);

        // If the purchase was successful, refresh the GUI to update the item's lore
        if (purchaseSuccess) {
            this.openShop(player, currentPage);
        }
    }

    /**
     * Cleans up player data when they quit the server.
     */
    public void clearPlayerData(Player player) {
        playerPages.remove(player.getUniqueId());
    }
}
