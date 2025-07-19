package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.handlers.BoostHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BoostListener implements Listener {
    private final BoostHandler boostHandler;

    public BoostListener(BoostHandler boostHandler) {
        this.boostHandler = boostHandler;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        boostHandler.handleInteract(player, player.isGliding(), player.isSneaking(), player.isOnGround());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        boostHandler.handlePlayerQuit(event);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        boostHandler.handleToggleSneak(event);
    }
}