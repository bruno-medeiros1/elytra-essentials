package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.Constants;
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
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ElytraEquipListener implements Listener {
    private final ElytraEssentials plugin;

    public ElytraEquipListener(ElytraEssentials plugin) {

        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (isArmorChangeEvent(event)) {
            scheduleArmorCheck(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.ELYTRA) {
            scheduleArmorCheck(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDispense(BlockDispenseArmorEvent event) {
        if (event.getItem().getType() == Material.ELYTRA && event.getTargetEntity() instanceof Player player) {
            scheduleArmorCheck(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        if (event.getBrokenItem().getType() == Material.ELYTRA) {
            scheduleArmorCheck(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // When a player dies, their armor is cleared.
        scheduleArmorCheck(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Check again on respawn, in case they keep their inventory.
        scheduleArmorCheck(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleArmorCheck(event.getPlayer());
    }

    private boolean isArmorChangeEvent(InventoryClickEvent event) {
        // Direct click in the chestplate slot
        if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == Constants.Inventory.CHESTPLATE_SLOT) {
            return true;
        }

        // Shift-clicking an item from the main inventory
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ELYTRA;
        }

        return false;
    }

    private void scheduleArmorCheck(Player player) {
        // Schedule the check for the next server tick to ensure the inventory has updated.
        plugin.getServer().getScheduler().runTask(plugin, () -> checkAndHandleElytraEquip(player));
    }

    private void checkAndHandleElytraEquip(Player player) {
        if (!plugin.getConfigHandlerInstance().getIsElytraEquipDisabled()) {
            return;
        }
        if (PermissionsHelper.PlayerBypassElytraEquip(player)) {
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();

        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getElytraEquipDisabled());

            player.getInventory().setChestplate(null);
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(chestplate);

            if (!leftovers.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), chestplate);
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getElytraEquipDropped());
            }
        }
    }
}