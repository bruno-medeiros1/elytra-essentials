package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.handlers.ElytraEquipHandler;
import org.bukkit.Material;
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

public class ElytraEquipListener implements Listener {
    private final ElytraEquipHandler elytraEquipHandler;

    public ElytraEquipListener(ElytraEquipHandler elytraEquipHandler) {
        this.elytraEquipHandler = elytraEquipHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) { elytraEquipHandler.scheduleEquipCheck(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) { elytraEquipHandler.scheduleEquipCheck(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getSlotType() == InventoryType.SlotType.ARMOR || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (event.getWhoClicked() instanceof Player player) {
                elytraEquipHandler.scheduleEquipCheck(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() != null && event.getItem().getType() == Material.ELYTRA) {
                elytraEquipHandler.scheduleEquipCheck(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDispense(BlockDispenseArmorEvent event) {
        if (event.getItem().getType() == Material.ELYTRA && event.getTargetEntity() instanceof Player player) {
            elytraEquipHandler.scheduleEquipCheck(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        if (event.getBrokenItem().getType() == Material.ELYTRA) {
            elytraEquipHandler.scheduleEquipCheck(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        elytraEquipHandler.scheduleEquipCheck(event.getEntity());
    }
}