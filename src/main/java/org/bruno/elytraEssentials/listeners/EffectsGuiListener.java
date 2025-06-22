package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.commands.EffectsCommand;
import org.bruno.elytraEssentials.commands.ShopCommand;
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

    // These should match the values in your EffectsCommand class
    private static final int SHOP_SLOT = 18;
    private static final int CLOSE_SLOT = 26;

    public EffectsGuiListener(ElytraEssentials plugin, EffectsCommand effectsCommand, ShopCommand shopCommand) {
        this.plugin = plugin;
        this.effectsCommand = effectsCommand;
        this.shopCommand = shopCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // We only care about clicks inside the EffectsHolder GUI
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof EffectsHolder)) {
            return;
        }

        // Always cancel the event to prevent players from taking items
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int clickedSlot = event.getSlot();

        switch (clickedSlot) {
            case SHOP_SLOT:
                player.closeInventory();
                shopCommand.OpenShop(player);
                return;

            case CLOSE_SLOT:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
                player.closeInventory();
                return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta != null) {
            String effectKey = meta.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "effect_key"),
                    PersistentDataType.STRING
            );
            // If the item has no effect key, it's a filler pane, so do nothing.
            if (effectKey == null) return;

            // Attempt to select the effect
            boolean selectionSuccess = plugin.getEffectsHandler().handleSelection(player, effectKey);
            if (selectionSuccess) {
                effectsCommand.OpenOwnedEffects(player);
            }
        }
    }
}