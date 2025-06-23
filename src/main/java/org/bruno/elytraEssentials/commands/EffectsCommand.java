package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.constants.GuiConstants;
import org.bruno.elytraEssentials.gui.EffectsHolder;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class EffectsCommand implements ISubCommand {
    private final ElytraEssentials plugin;

    public EffectsCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            return true;

        MessagesHelper messagesHelper = this.plugin.getMessagesHelper();
        boolean canOpen = PermissionsHelper.hasEffectsPermission(player);
        if (!canOpen) {
            messagesHelper.sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return true;
        }

        player.playSound(player.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 0.8f, 0.8f);
        OpenOwnedEffects(player);
        return true;
    }

    /**
     * Creates and opens the GUI showing a player's owned effects.
     * This version does not support pagination.
     * @param player The player to open the GUI for.
     */
    public void OpenOwnedEffects(Player player) {
        Inventory ownedEffects = Bukkit.createInventory(new EffectsHolder(), GuiConstants.EFFECTS_INVENTORY_SIZE, GuiConstants.EFFECTS_INVENTORY_NAME);

        try {
            List<String> playerOwnedKeys = plugin.getDatabaseHandler().GetOwnedEffectKeys(player.getUniqueId());

            // Add the static control buttons to the bottom row
            addControlButtons(ownedEffects);

            if (playerOwnedKeys.isEmpty()) {
                ItemStack item = plugin.getEffectsHandler().createEmptyItemStack();
                ownedEffects.setItem(13, item); // Center slot
            } else {
                populateOwnedItems(ownedEffects, player, playerOwnedKeys);
            }

            player.openInventory(ownedEffects);

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while fetching your effects.");
            e.printStackTrace();
        }
    }

    //  Fills the GUI with the player's owned effects, up to the display limit.
    private void populateOwnedItems(Inventory inv, Player player, List<String> playerOwnedKeys) throws SQLException {
        Map<String, ElytraEffect> allEffects = plugin.getEffectsHandler().getEffectsRegistry();

        for (int i = 0; i < playerOwnedKeys.size(); i++) {
            if (i >= GuiConstants.EFFECTS_ITEM_DISPLAY_LIMIT)
                break;

            String effectKey = playerOwnedKeys.get(i);
            ElytraEffect elytraEffect = allEffects.get(effectKey);

            if (elytraEffect != null) {
                boolean isActive = plugin.getDatabaseHandler().GetIsActiveOwnedEffect(player.getUniqueId(), effectKey);
                elytraEffect.setIsActive(isActive);
                ItemStack item = plugin.getEffectsHandler().createOwnedItem(effectKey, elytraEffect);
                inv.setItem(i, item);
            }
        }
    }

    //  Creates and places the static control buttons at the bottom of the GUI.
    private void addControlButtons(Inventory inv) {
        inv.setItem(GuiConstants.EFFECTS_SHOP_SLOT, GuiHelper.createGuiItem(Material.CHEST, "§aShop", "§7Click here to buy more effects."));
        inv.setItem(GuiConstants.EFFECTS_PREVIOUS_PAGE_SLOT, GuiHelper.createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cPrevious Page", "§7You are on the first page."));
        inv.setItem(GuiConstants.EFFECTS_PAGE_INFO_SLOT, GuiHelper.createGuiItem(Material.COMPASS, "§ePage 1/1"));
        inv.setItem(GuiConstants.EFFECTS_NEXT_PAGE_SLOT, GuiHelper.createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§aNext Page", "§7You are on the last page."));
        inv.setItem(GuiConstants.EFFECTS_CLOSE_SLOT, GuiHelper.createGuiItem(Material.BARRIER, "§cClose Menu", "§7Click to exit."));
    }
}