package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.commands.EffectsCommand;
import org.bruno.elytraEssentials.commands.ShopCommand;
import org.bruno.elytraEssentials.gui.ShopHolder;
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

public class ShopGuiListener implements Listener {

    private static final int PLAYER_HEAD_SLOT = 27;
    private static final int CLOSE_SLOT = 35;
    // TODO: Add other slots like NEXT_PAGE_SLOT when pagination is implemented

    private final ElytraEssentials plugin;
    private final EffectsCommand effectsCommand;
    private final ShopCommand shopCommand;

    public ShopGuiListener(ElytraEssentials plugin, EffectsCommand effectsCommand, ShopCommand shopCommand) {
        this.plugin = plugin;
        this.effectsCommand = effectsCommand;
        this.shopCommand = shopCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // We only care about clicks inside the ShopHolder GUI
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof ShopHolder))
            return;

        event.setCancelled(true); // Always cancel events in our GUI

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        int clickedSlot = event.getSlot();

        // Handle static control buttons
        switch (clickedSlot) {
            case CLOSE_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
                return;

            case PLAYER_HEAD_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                effectsCommand.OpenOwnedEffects(player);
                return;
        }

        // If it wasn't a control button, it must be an effect item. This is the purchase logic.
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta != null) {
            String effectKey = meta.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "effect_key"),
                    PersistentDataType.STRING
            );
            if (effectKey == null) return; // It was probably a filler pane

            String permission = meta.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "effect_permission"),
                    PersistentDataType.STRING
            );

            boolean purchaseSuccess = plugin.getEffectsHandler().handlePurchase(player, effectKey, permission);
            if (purchaseSuccess) {
                shopCommand.OpenShop(player);
            }
        }
    }
}