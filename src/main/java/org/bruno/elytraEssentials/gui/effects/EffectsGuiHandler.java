package org.bruno.elytraEssentials.gui.effects;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.gui.shop.ShopGuiHandler;
import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.handlers.EffectsHandler;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
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

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class EffectsGuiHandler {
    private final ElytraEssentials plugin;
    private final EffectsHandler effectsHandler;
    private final DatabaseHandler databaseHandler;
    private final FoliaHelper foliaHelper;
    private final MessagesHelper messagesHelper;

    private ShopGuiHandler shopGuiHandler;

    public EffectsGuiHandler(ElytraEssentials plugin, EffectsHandler effectsHandler, DatabaseHandler databaseHandler, FoliaHelper foliaHelper, MessagesHelper messagesHelper) {
        this.plugin = plugin;
        this.effectsHandler = effectsHandler;
        this.databaseHandler = databaseHandler;
        this.foliaHelper = foliaHelper;
        this.messagesHelper = messagesHelper;
    }

    public void setShopGuiHandler(ShopGuiHandler shopGuiHandler) {
        this.shopGuiHandler = shopGuiHandler;
    }

    /**
     * Asynchronously builds and opens the "Owned Effects" GUI for a player.
     */
    public void open(Player player) {
        foliaHelper.runAsyncTask(() -> {
            try {
                // Fetch all owned effect keys from the database and permissions
                Set<String> ownedKeys = new HashSet<>(databaseHandler.GetOwnedEffectKeys(player.getUniqueId()));

                List<String> keysToDisplay = new ArrayList<>(ownedKeys);

                // Now that we have the data, switch back to the main thread to create and open the GUI
                foliaHelper.runTaskOnMainThread(() -> {
                    Inventory gui = Bukkit.createInventory(new EffectsHolder(), Constants.GUI.EFFECTS_INVENTORY_SIZE, Constants.GUI.EFFECTS_INVENTORY_NAME);
                    addControlButtons(gui);

                    if (keysToDisplay.isEmpty()) {
                        gui.setItem(13, effectsHandler.createEmptyItemStack());
                    } else {
                        populateOwnedItems(gui, player, keysToDisplay);
                    }
                    player.openInventory(gui);
                });

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to open owned effects GUI for " + player.getName(), e);
                foliaHelper.runTaskOnMainThread(() ->
                        messagesHelper.sendPlayerMessage(player, "&cAn error occurred while fetching your effects."));
            }
        });
    }

    /**
     * Handles all click events for the effects GUI.
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int clickedSlot = event.getSlot();

        switch (clickedSlot) {
            case Constants.GUI.EFFECTS_SHOP_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);

                shopGuiHandler.openShop(player, 0);
                return;

            case Constants.GUI.EFFECTS_CLOSE_SLOT:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
                player.closeInventory();
                return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        // This key should ideally be stored in your Constants class
        String effectKey = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, Constants.NBT.EFFECT_KEY),
                PersistentDataType.STRING
        );
        if (effectKey == null) return;

        if (event.isLeftClick()) {
            // Player wants to SELECT a new effect.
            boolean selectionSuccess = plugin.getEffectsHandler().handleSelection(player, effectKey);
            if (selectionSuccess) {
                this.open(player);
            }
        } else if (event.isRightClick()) {
            // Player wants to DESELECT their active effect.
            boolean deselectionSuccess = plugin.getEffectsHandler().handleDeselection(player, effectKey);
            if (deselectionSuccess) {
                // Tell the main plugin to refresh the GUI
                this.open(player);
            }
        }
    }

    private void populateOwnedItems(Inventory inv, Player player, List<String> playerOwnedKeys) {
        Map<String, ElytraEffect> allEffects = plugin.getEffectsHandler().getEffectsRegistry();
        String activeEffectKey = plugin.getEffectsHandler().getActiveEffect(player.getUniqueId());

        for (int i = 0; i < playerOwnedKeys.size(); i++) {
            if (i >= Constants.GUI.EFFECTS_ITEM_DISPLAY_LIMIT) break;

            String effectKey = playerOwnedKeys.get(i);
            ElytraEffect templateEffect = allEffects.get(effectKey);

            if (templateEffect != null) {
                ElytraEffect playerSpecificEffect = new ElytraEffect(templateEffect);
                playerSpecificEffect.setIsActive(effectKey.equals(activeEffectKey));
                ItemStack item = plugin.getEffectsHandler().createOwnedItem(effectKey, playerSpecificEffect);
                inv.setItem(i, item);
            }
        }
    }

    private void addControlButtons(Inventory inv) {
        inv.setItem(Constants.GUI.EFFECTS_SHOP_SLOT, GuiHelper.createGuiItem(Material.CHEST, "§aShop", "§7Click here to buy more effects."));
        inv.setItem(Constants.GUI.EFFECTS_PREVIOUS_PAGE_SLOT, GuiHelper.createPreviousPageButton(false));
        inv.setItem(Constants.GUI.EFFECTS_PAGE_INFO_SLOT, GuiHelper.createPageInfoItem(1, 1));
        inv.setItem(Constants.GUI.EFFECTS_NEXT_PAGE_SLOT, GuiHelper.createNextPageButton(false));
        inv.setItem(Constants.GUI.EFFECTS_CLOSE_SLOT, GuiHelper.createCloseButton());
    }
}
