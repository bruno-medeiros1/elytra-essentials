package org.bruno.elytraEssentials.gui.forge;

import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.*;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.UpgradeType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ForgeGuiHandler {
    private final ElytraEssentials plugin;
    private final ConfigHandler configHandler;
    private final ArmoredElytraHelper armoredElytraHelper;
    private final FoliaHelper foliaHelper;
    private final MessagesHandler messagesHandler;
    private final MessagesHelper messagesHelper;
    private final Economy economy;

    // Use a thread-safe set for Folia compatibility
    private final Set<UUID> processedAction = ConcurrentHashMap.newKeySet();

    public ForgeGuiHandler(ConfigHandler configHandler, ArmoredElytraHelper armoredElytraHelper, FoliaHelper foliaHelper,
                           MessagesHandler messagesHandler, MessagesHelper messagesHelper, Economy economy, ElytraEssentials plugin) {
        this.plugin = plugin;
        this.configHandler = configHandler;
        this.armoredElytraHelper = armoredElytraHelper;
        this.foliaHelper = foliaHelper;
        this.messagesHandler = messagesHandler;
        this.messagesHelper = messagesHelper;
        this.economy = economy;
    }

    /**
     * Creates and opens the forge GUI for a player.
     * Logic moved from the old ForgeCommand.
     */
    public void openForge(Player player) {
        Inventory forge = Bukkit.createInventory(new ForgeHolder(), Constants.GUI.FORGE_INVENTORY_SIZE, Constants.GUI.FORGE_INVENTORY_NAME);

        ItemStack grayPane = GuiHelper.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        // First, fill the entire inventory with the gray filler pane
        for (int i = 0; i < Constants.GUI.FORGE_INVENTORY_SIZE - 9; i++) {
            forge.setItem(i, grayPane);
        }

        // This creates the layout with inputs on the sides and the result in the middle.
        forge.setItem(Constants.GUI.FORGE_ELYTRA_SLOT, null);
        forge.setItem(Constants.GUI.FORGE_ARMOR_SLOT, null);
        forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, null);

        // Place the instructional anvil item at the bottom
        ItemStack infoAnvil = GuiHelper.createGuiItem(Material.ANVIL,
                "§6§lElytra Forge",
                "§7Combine an elytra with a chestplate to create",
                "§7a powerful new item, or place an Armored Elytra",
                "§7in the middle slot to revert it back.",
                "",
                "§f▪ §eForge: §7Place items in the side slots.",
                "§f▪ §eRevert: §7Place an Armored Elytra in the middle."
        );
        forge.setItem(Constants.GUI.FORGE_INFO_ANVIL_SLOT, infoAnvil);

        // Place informational and action items
        if (economy != null) {
            forge.setItem(Constants.GUI.FORGE_HEAD_SLOT, GuiHelper.createPlayerHead(player, "§6Your Balance", "§e$" + String.format("%,.2f", economy.getBalance(player))));
        }
        forge.setItem(Constants.GUI.FORGE_CLOSE_SLOT, GuiHelper.createCloseButton());

        // Open the GUI for the player
        player.openInventory(forge);
    }

    /**
     * Handles all click events for the forge GUI.
     * Logic moved from the old ForgeGuiListener.
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!configHandler.getIsArmoredElytraEnabled()) return;

        Inventory topInventory = GuiHelper.getTopInventory(event);
        if (topInventory.getHolder() == null || !(topInventory.getHolder() instanceof ForgeHolder)) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // We schedule an update after every click to keep the GUI state correct.
        // The one-tick delay allows Bukkit to process the click action first.
        scheduleResultUpdate(topInventory, player);

        Inventory clickedInventory = event.getClickedInventory();

        // Handle Shift-Clicks from the Player's Inventory
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (clickedInventory != null && clickedInventory.equals(player.getInventory())) {
                event.setCancelled(true);
                handleShiftClick(event.getCurrentItem(), topInventory);
                return;
            }
        }

        if (topInventory.equals(clickedInventory)) {
            int slot = event.getSlot();
            ItemStack clickedItem = event.getCurrentItem();

            switch (slot) {
                case Constants.GUI.FORGE_CONFIRM_SLOT:
                    event.setCancelled(true);
                    //  Only handle click if the confirm button is actually there
                    if (clickedItem != null && (clickedItem.getType() == Material.PLAYER_HEAD || clickedItem.getType() == Material.GREEN_STAINED_GLASS_PANE)) {
                        handleConfirmClick(topInventory, player);
                    }
                    break;
                case Constants.GUI.FORGE_CANCEL_SLOT:
                    event.setCancelled(true);
                    if (clickedItem != null && (clickedItem.getType() == Material.PLAYER_HEAD || clickedItem.getType() == Material.RED_STAINED_GLASS_PANE)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 0.8f);
                        handleCancelClick(topInventory, player);
                    }
                    break;
                case Constants.GUI.FORGE_CLOSE_SLOT:
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
                    handleCancelClick(topInventory, player);
                    player.closeInventory();
                    break;
                default:
                    event.setCancelled(true);
                    break;
            }
        }
    }

    /**
     * Cleans up player data when they quit the server.
     */
    public void clearPlayerData(Player player) {
        processedAction.remove(player.getUniqueId());
    }

    public void handleClose(InventoryCloseEvent event){
        Player player = (Player) event.getPlayer();

        if (processedAction.remove(player.getUniqueId()))
            return;

        Inventory forge = event.getInventory();
        returnAllItems(forge, player);
    }

    /**
     * Schedules a result update using the Folia-safe helper.
     */
    private void scheduleResultUpdate(Inventory forge, Player player) {
        // This is now Folia-safe
        foliaHelper.runTask(player, () -> updateForgeResult(forge, player));
    }

    private void returnAllItems(Inventory forge, Player player) {
        returnItemToPlayer(player, forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT));
        returnItemToPlayer(player, forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT));
        returnItemToPlayer(player, forge.getItem(Constants.GUI.FORGE_RESULT_SLOT));
    }

    private void returnItemToPlayer(Player player, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && !armoredElytraHelper.isPreviewItem(item)) {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            if (!leftovers.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }
    }

    private void handleConfirmClick(Inventory forge, Player player) {
        ItemStack resultItem = forge.getItem(Constants.GUI.FORGE_RESULT_SLOT);
        ItemStack revertedElytra = forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT);
        ItemStack revertedArmor = forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT);

        processedAction.add(player.getUniqueId());

        double moneyCost = configHandler.getForgeCostMoney();
        int xpCost = configHandler.getForgeCostXpLevels();

        boolean isCrafting = armoredElytraHelper.isPreviewItem(resultItem);
        boolean isReverting = armoredElytraHelper.isPreviewItem(revertedElytra) && armoredElytraHelper.isPreviewItem(revertedArmor);

        if (!isCrafting && !isReverting){
            returnAllItems(forge, player);
            forge.clear();
            player.closeInventory();
            return;
        }

        if (!canAfford(player, moneyCost, xpCost)){
            returnAllItems(forge, player);
            forge.clear();
            player.closeInventory();
            return;
        }

        deductCosts(player, moneyCost, xpCost);

        if (isCrafting) {
            returnItemToPlayer(player, armoredElytraHelper.createCleanCopy(resultItem));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            messagesHelper.sendPlayerMessage(player, messagesHandler.getForgeSuccessful());
        } else {
            returnItemToPlayer(player, armoredElytraHelper.createCleanCopy(revertedElytra));
            returnItemToPlayer(player, armoredElytraHelper.createCleanCopy(revertedArmor));
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.0f);
            messagesHelper.sendPlayerMessage(player, messagesHandler.getRevertSuccessful());
        }

        forge.clear();
        player.closeInventory();
    }

    private boolean canAfford(Player player, double moneyCost, int xpCost) {
        if (moneyCost > 0) {
            if (economy == null) {
                plugin.getLogger().warning("Economy not found while handling forge!");
                return false;
            }

            if (!economy.has(player, moneyCost)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
                messagesHelper.sendPlayerMessage(player, messagesHandler.getNotEnoughMoney());
                return false;
            }
        }

        if (xpCost > 0 && player.getLevel() < xpCost) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
            messagesHelper.sendPlayerMessage(player, messagesHandler.getNotEnoughXP());
            return false;
        }

        return true;
    }

    private void deductCosts(Player player, double moneyCost, int xpCost) {
        if (moneyCost > 0) economy.withdrawPlayer(player, moneyCost);
        if (xpCost > 0) player.setLevel(player.getLevel() - xpCost);
    }

    private void handleShiftClick(ItemStack clickedItem, Inventory forge) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemStack singleItem = clickedItem.clone();
        singleItem.setAmount(1);

        if (armoredElytraHelper.isPlainElytra(clickedItem) && isEmpty(forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT))) {
            forge.setItem(Constants.GUI.FORGE_ELYTRA_SLOT, singleItem);
            clickedItem.setAmount(clickedItem.getAmount() - 1);
        } else if (armoredElytraHelper.isChestplate(clickedItem) && isEmpty(forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT))) {
            forge.setItem(Constants.GUI.FORGE_ARMOR_SLOT, singleItem);
            clickedItem.setAmount(clickedItem.getAmount() - 1);
        } else if (armoredElytraHelper.isArmoredElytra(clickedItem) && isEmpty(forge.getItem(Constants.GUI.FORGE_RESULT_SLOT))) {
            forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, singleItem);
            clickedItem.setAmount(clickedItem.getAmount() - 1);
        }
    }

    private void updateForgeResult(Inventory forge, Player player) {
        ItemStack elytraSlot = forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT);
        ItemStack armorSlot = forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT);
        ItemStack middleSlot = forge.getItem(Constants.GUI.FORGE_RESULT_SLOT);

        boolean isRevertible = armoredElytraHelper.isArmoredElytra(middleSlot) && !armoredElytraHelper.isPreviewItem(middleSlot) && isEmpty(elytraSlot) && isEmpty(armorSlot);
        boolean isCraftable = armoredElytraHelper.isPlainElytra(elytraSlot) && armoredElytraHelper.isChestplate(armorSlot);

        if (isRevertible) {
            displayRevertedItems(forge, middleSlot);
            showActionButtons(forge, "Revert", middleSlot);
        } else if (isCraftable) {
            if (elytraSlot == null) return;
            ItemStack armoredElytra = armoredElytraHelper.createArmoredElytra(elytraSlot, armorSlot, player);
            forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, armoredElytra);
            showActionButtons(forge, "Forge", null);
        } else {
            // Not a valid recipe. Clear any preview items.
            if (armoredElytraHelper.isPreviewItem(forge.getItem(Constants.GUI.FORGE_RESULT_SLOT))) {
                forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, null);
            }
            if (armoredElytraHelper.isPreviewItem(forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT))) {
                forge.setItem(Constants.GUI.FORGE_ELYTRA_SLOT, null);
                forge.setItem(Constants.GUI.FORGE_ARMOR_SLOT, null);
            }

            // Check if there are any items left to cancel.
            boolean hasInputs = !isEmpty(forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT)) ||
                    !isEmpty(forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT)) ||
                    !isEmpty(forge.getItem(Constants.GUI.FORGE_RESULT_SLOT));
            updateActionButtons(forge, hasInputs);
        }
    }

    private void showActionButtons(Inventory forge, String action, ItemStack armoredElytra) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to complete the process.");

        if ("Revert".equals(action) && armoredElytra != null && armoredElytra.hasItemMeta()) {
            ItemMeta meta = armoredElytra.getItemMeta();
            if (meta != null) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                boolean hasUpgrades = false;
                for (UpgradeType type : UpgradeType.values()) {
                    if (container.has(new NamespacedKey(plugin, type.getKey()), PersistentDataType.INTEGER)) {
                        hasUpgrades = true;
                        break;
                    }
                }
                if (hasUpgrades) {
                    lore.add("");
                    lore.add(ColorHelper.parse("&c&lWARNING:"));
                    lore.add(ColorHelper.parse("&#FFBFBFThis item has upgrades that will be"));
                    lore.add(ColorHelper.parse("&#FFBFBFpermanently lost upon reverting!"));
                }
            }
        }

        double moneyCost = configHandler.getForgeCostMoney();
        int xpCost = configHandler.getForgeCostXpLevels();

        if (moneyCost > 0 || xpCost > 0) {
            lore.add("");
            lore.add("§6Requirements:");
            if (moneyCost > 0) lore.add(String.format(" §f▪ §e$%.2f", moneyCost));
            if (xpCost > 0) lore.add(String.format(" §f▪ §e%d Levels", xpCost));
        }

        ItemStack confirmButton = GuiHelper.createAcceptButton(action, lore);
        ItemStack cancelButton = GuiHelper.createCancelButton();

        forge.setItem(Constants.GUI.FORGE_CONFIRM_SLOT, confirmButton);
        forge.setItem(Constants.GUI.FORGE_CANCEL_SLOT, cancelButton);
    }

    private void updateActionButtons(Inventory forge, boolean hasInputs) {
        if (hasInputs) {
            forge.setItem(Constants.GUI.FORGE_CANCEL_SLOT, GuiHelper.createCancelButton());
        } else {
            forge.setItem(Constants.GUI.FORGE_CANCEL_SLOT, null);
            forge.setItem(Constants.GUI.FORGE_CONFIRM_SLOT, null);
        }
    }

    /**
     * Handles the logic for the "Cancel" button, returning items and resetting the GUI.
     */
    private void handleCancelClick(Inventory forge, Player player) {
        // Return all items that are not preview items back to the player
        returnAllItems(forge, player);

        // Clear the interactive slots to reset the GUI state
        forge.setItem(Constants.GUI.FORGE_ELYTRA_SLOT, null);
        forge.setItem(Constants.GUI.FORGE_ARMOR_SLOT, null);
        forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, null);
    }

    private void displayRevertedItems(Inventory forge, ItemStack armoredElytra) {
        ItemStack plainElytra = armoredElytraHelper.reassembleElytra(armoredElytra);
        ItemStack chestplate = armoredElytraHelper.reassembleChestplate(armoredElytra);

        armoredElytraHelper.addPreviewTag(plainElytra);
        armoredElytraHelper.addPreviewTag(chestplate);

        forge.setItem(Constants.GUI.FORGE_ELYTRA_SLOT, plainElytra);
        forge.setItem(Constants.GUI.FORGE_ARMOR_SLOT, chestplate);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }
}
