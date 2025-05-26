package org.bruno.elytraEssentials.listeners;

import com.github.jewishbanana.playerarmorchangeevent.PlayerArmorChangeEvent;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ElytraEquipListener implements Listener {
    private final ElytraEssentials elytraEssentials;

    public ElytraEquipListener(ElytraEssentials elytraEssentials) {
        this.elytraEssentials = elytraEssentials;
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent e) {
        Player player = e.getPlayer();
        ConfigHandler configHandler = elytraEssentials.getConfigHandlerInstance();

        if (e.getNewItem().getType() == Material.ELYTRA && configHandler.getIsElytraEquipDisabled() && !PlayerBypassEquipElytraRestriction(player)) {
            e.setCancelled(true);
            MessagesHelper.sendPlayerMessage(player, this.elytraEssentials.getMessagesHandlerInstance().getElytraEquipDisabledMessage());
        }
    }

    //TODO: Add VersionManager check for newer versions
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigHandler configHandler = elytraEssentials.getConfigHandlerInstance();

        if (configHandler.getIsElytraEquipDisabled() && !PlayerBypassEquipElytraRestriction(player)) {
            ItemStack chestplate = player.getInventory().getChestplate();

            // Check if the chestplate is an Elytra
            if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                player.getInventory().setChestplate(null); // Remove Elytra from chestplate slot
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(chestplate);

                // If there are leftovers (no space in inventory), drop the Elytra on the ground
                if (!leftover.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), chestplate);
                    MessagesHelper.sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraEquipDroppedMessage());
                }
                else{
                    MessagesHelper.sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraEquipReturnedMessage());
                }
            }
        }
    }

    private boolean PlayerBypassEquipElytraRestriction(Player player) {
        return player.hasPermission("elytraessentials.bypass.equipment") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                player.hasPermission("elytraessentials.*");
    }
}
