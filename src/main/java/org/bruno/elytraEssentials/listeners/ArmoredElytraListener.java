package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.handlers.ArmoredElytraHandler;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

//  Apply or remove the player's armor stat buffs.
public class ArmoredElytraListener implements Listener {
    private final ArmoredElytraHandler armoredElytraHandler;
    private final ConfigHandler configHandler;

    public ArmoredElytraListener(ArmoredElytraHandler armoredElytraHandler, ConfigHandler configHandler) {
        this.armoredElytraHandler = armoredElytraHandler;
        this.configHandler = configHandler;
    }

    private boolean isDisabled() {
        return !configHandler.getIsArmoredElytraEnabled();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (isDisabled() || !(e.getWhoClicked() instanceof Player player)) return;

        if (e.getSlotType() == InventoryType.SlotType.ARMOR || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            armoredElytraHandler.scheduleArmorCheck(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (isDisabled() || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        armoredElytraHandler.scheduleArmorCheck(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isDisabled()) return;
        armoredElytraHandler.removeArmorAttributes(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        if (isDisabled()) return;
        armoredElytraHandler.removeArmorAttributes(event.getPlayer());
    }

    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) {
        if (isDisabled()) return;
        armoredElytraHandler.scheduleArmorCheck(event.getPlayer());
    }

    @EventHandler public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (isDisabled()) return;
        armoredElytraHandler.scheduleArmorCheck(event.getPlayer());
    }

    @EventHandler public void onDispense(BlockDispenseArmorEvent event) {
        if (isDisabled() || !(event.getTargetEntity() instanceof Player p)) return;
        armoredElytraHandler.scheduleArmorCheck(p);
    }
}