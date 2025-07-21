package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ElytraEquipHandler {
    private final ConfigHandler configHandler;
    private final MessagesHelper messagesHelper;
    private final FoliaHelper foliaHelper;
    private final MessagesHandler messagesHandler;

    public ElytraEquipHandler(ConfigHandler configHandler, MessagesHelper messagesHelper, FoliaHelper foliaHelper, MessagesHandler messagesHandler) {
        this.configHandler = configHandler;
        this.messagesHelper = messagesHelper;
        this.foliaHelper = foliaHelper;
        this.messagesHandler = messagesHandler;
    }

    /**
     * Schedules a check for the next server tick to ensure the inventory has updated.
     * This is the main entry point called by the listener.
     */
    public void scheduleEquipCheck(Player player) {
        // Use the Folia-safe, entity-specific scheduler
        foliaHelper.runTask(player, () -> checkAndHandleElytraEquip(player));
    }

    /**
     * The core logic. Checks if a player has an elytra equipped and removes it if disallowed.
     */
    private void checkAndHandleElytraEquip(Player player) {
        if (!configHandler.getIsElytraEquipDisabled()) return;
        if (PermissionsHelper.playerBypassElytraEquip(player)) return;

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
            messagesHelper.sendPlayerMessage(player, messagesHandler.getElytraEquipDisabled());

            player.getInventory().setChestplate(null);
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(chestplate);

            if (!leftovers.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), chestplate);
                messagesHelper.sendPlayerMessage(player, messagesHandler.getElytraEquipDropped());
            }
        }
    }
}
