package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.gui.EffectsHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class EffectsGuiListener implements Listener {

    private final ElytraEssentials plugin;

    public EffectsGuiListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof EffectsHolder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int clickedSlot = event.getSlot();

        switch (clickedSlot) {
            case Constants.GUI.EFFECTS_SHOP_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);

                plugin.openShopGUI(player, 0);
                return;

            case Constants.GUI.EFFECTS_CLOSE_SLOT:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
                player.closeInventory();
                return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        // This key should ideally be stored in your Constants class
        String effectKey = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, Constants.NBT.EFFECT_KEY),
                PersistentDataType.STRING
        );
        if (effectKey == null) return;

        if (event.isLeftClick()) {
            // Player wants to SELECT a new effect.
            boolean selectionSuccess = plugin.getEffectsHandler().handleSelection(player, effectKey);
            if (selectionSuccess) {
                // Tell the main plugin to refresh the GUI
                plugin.openEffectsGUI(player);
            }
        } else if (event.isRightClick()) {
            // Player wants to DESELECT their active effect.
            boolean deselectionSuccess = plugin.getEffectsHandler().handleDeselection(player, effectKey);
            if (deselectionSuccess) {
                // Tell the main plugin to refresh the GUI
                plugin.openEffectsGUI(player);
            }
        }
    }
}