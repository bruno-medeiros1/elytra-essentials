package org.bruno.elytraEssentials.listeners;

import com.github.jewishbanana.playerarmorchangeevent.PlayerArmorChangeEvent;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
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

        if (e.getNewItem().getType() == Material.ELYTRA && configHandler.getIsElytraEquipDisabled() && !PermissionsHelper.PlayerBypassElytraEquip(player)) {
            e.setCancelled(true);
            this.elytraEssentials.getMessagesHelper().sendPlayerMessage(player, this.elytraEssentials.getMessagesHandlerInstance().getElytraEquipDisabled());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigHandler configHandler = elytraEssentials.getConfigHandlerInstance();

        if (configHandler.getIsElytraEquipDisabled() && !PermissionsHelper.PlayerBypassElytraEquip(player)) {
            ItemStack chestplate = player.getInventory().getChestplate();

            // Check if the chestplate is an Elytra
            if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                player.getInventory().setChestplate(null); // Remove Elytra from chestplate slot
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(chestplate);

                // If there are leftovers (no space in inventory), drop the Elytra on the ground
                if (!leftover.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), chestplate);
                    this.elytraEssentials.getMessagesHelper().sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraEquipDropped());
                }
                else{
                    this.elytraEssentials.getMessagesHelper().sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraEquipReturned());
                }
            }
        }
    }
}
