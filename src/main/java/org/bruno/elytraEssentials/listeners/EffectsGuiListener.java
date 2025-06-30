package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.commands.EffectsCommand;
import org.bruno.elytraEssentials.commands.ShopCommand;
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
    private final EffectsCommand effectsCommand;
    private final ShopCommand shopCommand;

    public EffectsGuiListener(ElytraEssentials plugin, EffectsCommand effectsCommand, ShopCommand shopCommand) {
        this.plugin = plugin;
        this.effectsCommand = effectsCommand;
        this.shopCommand = shopCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof EffectsHolder))
            return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int clickedSlot = event.getSlot();

        switch (clickedSlot) {
            case Constants.GUI.EFFECTS_SHOP_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);
                shopCommand.OpenShop(player, 0);
                return;

            case Constants.GUI.EFFECTS_CLOSE_SLOT:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
                player.closeInventory();
                return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        String effectKey = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "effect_key"),
                PersistentDataType.STRING
        );
        if (effectKey == null) return; // It's a filler pane or control button without a key.

        if (event.isLeftClick()) {
            // Player wants to SELECT a new effect.
            boolean selectionSuccess = plugin.getEffectsHandler().handleSelection(player, effectKey);
            if (selectionSuccess)
                effectsCommand.OpenOwnedEffects(player); // Refresh GUI to show new active effect

        } else if (event.isRightClick()) {
            // Player wants to DESELECT their active effect.
            boolean deselectionSuccess = plugin.getEffectsHandler().handleDeselection(player, effectKey);
            if (deselectionSuccess)
                effectsCommand.OpenOwnedEffects(player); // Refresh GUI to show it's no longer active

        }
    }
}