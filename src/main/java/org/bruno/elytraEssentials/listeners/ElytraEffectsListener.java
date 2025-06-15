package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.gui.EffectsHolder;
import org.bruno.elytraEssentials.gui.ShopHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ElytraEffectsListener implements Listener {

    private final ElytraEssentials plugin;

    public ElytraEffectsListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof ShopHolder) {
            event.setCancelled(true); // Prevent any interaction within the shop GUI

            if (event.getClick().isShiftClick() || event.getClick().isCreativeAction()) {
                return; // Prevent shift-clicking or other unintended interactions
            }

            // Check if the clicked item is valid
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Prevent moving items to/from the shop GUI
            if (event.getClickedInventory() != event.getWhoClicked().getInventory()) {
                event.setCancelled(true);
            }

            // Retrieve the effect key from the item's metadata
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null) {
                String effectKey= meta.getPersistentDataContainer().get(
                        new NamespacedKey(plugin, "effect_key"),
                        PersistentDataType.STRING
                );
                if (effectKey == null) return;

                String permission = meta.getPersistentDataContainer().get(
                        new NamespacedKey(plugin, "effect_permission"),
                        PersistentDataType.STRING
                );

                plugin.getEffectsHandler().handlePurchase(player, effectKey, permission);
            }

            // Close the inventory
            // player.closeInventory();
        }

        if (inventory.getHolder() instanceof EffectsHolder) {
            event.setCancelled(true);

            if (event.getClick().isShiftClick() || event.getClick().isCreativeAction()) {
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            if (event.getClickedInventory() != event.getWhoClicked().getInventory()) {
                event.setCancelled(true);
            }

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null) {
                String effectKey = meta.getPersistentDataContainer().get(
                        new NamespacedKey(plugin, "effect_key"),
                        PersistentDataType.STRING
                );
                if (effectKey == null) return;

                plugin.getEffectsHandler().handleSelection(player, effectKey);
            }
        }
    }
}
