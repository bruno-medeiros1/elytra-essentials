package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ArmoredElytraHelper;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.gui.ForgeHolder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

//  Handle player interactions within the forge menu.
public class ForgeGuiListener implements Listener {

    private final ElytraEssentials plugin;
    private final ArmoredElytraHelper armoredElytraHelper;
    private final Set<UUID> processedAction = new HashSet<>();

    public ForgeGuiListener(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.armoredElytraHelper = plugin.getArmoredElytraHelper();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled()) return;

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
                        handleCancelClick(topInventory, player);
                    }
                    break;
                default:
                    event.setCancelled(true);
                    break;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof ForgeHolder)) return;

        if (processedAction.remove(event.getPlayer().getUniqueId())) return;

        Player player = (Player) event.getPlayer();
        Inventory forge = event.getInventory();

        returnAllItems(forge, player);
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

        if (armoredElytraHelper.isPreviewItem(resultItem)) { // Crafting
            returnItemToPlayer(player, armoredElytraHelper.createCleanCopy(resultItem));
            forge.clear();
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.closeInventory();
        } else if (armoredElytraHelper.isPreviewItem(revertedElytra) && armoredElytraHelper.isPreviewItem(revertedArmor)) { // Reverting
            returnItemToPlayer(player, armoredElytraHelper.createCleanCopy(revertedElytra));
            returnItemToPlayer(player, armoredElytraHelper.createCleanCopy(revertedArmor));
            forge.clear();
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.0f);
            player.closeInventory();
        }
    }

    private void scheduleResultUpdate(Inventory forge, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateForgeResult(forge, player);
            }
        }.runTask(plugin);
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
        boolean isCraftable = armoredElytraHelper.isPlainElytra(elytraSlot) && plugin.getArmoredElytraHelper().isChestplate(armorSlot);

        if (isRevertible) {
            displayRevertedItems(forge, middleSlot);
            showActionButtons(forge, "Revert");
        } else if (isCraftable) {
            if (elytraSlot == null) return;
            ItemStack armoredElytra = armoredElytraHelper.createArmoredElytra(elytraSlot, armorSlot, player);
            forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, armoredElytra);
            showActionButtons(forge, "Forge");
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

    private void showActionButtons(Inventory forge, String action) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to complete the process.");

        double moneyCost = plugin.getConfigHandlerInstance().getForgeCostMoney();
        int xpCost = plugin.getConfigHandlerInstance().getForgeCostXpLevels();

        if (moneyCost > 0 || xpCost > 0) {
            lore.add("");
            lore.add("§6Requirements:");
            if (moneyCost > 0) lore.add(String.format(" §f- §e$%.2f", moneyCost));
            if (xpCost > 0) lore.add(String.format(" §f- §b%d Levels", xpCost));
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

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 0.8f);
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