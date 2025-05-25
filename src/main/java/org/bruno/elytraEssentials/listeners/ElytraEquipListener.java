package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class ElytraEquipListener implements Listener {
    private final ElytraEssentials elytraEssentials;

    public ElytraEquipListener(ElytraEssentials elytraEssentials) {
        this.elytraEssentials = elytraEssentials;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ConfigHandler configHandler = this.elytraEssentials.getConfigHandlerInstance();

        if (e.getItem() != null && e.getItem().getType() == Material.ELYTRA) {
            if (configHandler.getDisableAllElytraFlight() || (configHandler.getDisabledWorlds() != null && configHandler.getDisabledWorlds().contains(player.getWorld().getName()))) {
                e.setCancelled(true);
                MessagesHelper.sendPlayerMessage(player, this.elytraEssentials.getMessagesHandlerInstance().getEquipElytraDisabledMessage());
            }
        }
    }
}
