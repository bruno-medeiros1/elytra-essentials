package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.commands.EffectsCommand;
import org.bruno.elytraEssentials.commands.ShopCommand;
import org.bruno.elytraEssentials.constants.GuiConstants;
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
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof ShopHolder))
            return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int clickedSlot = event.getSlot();

        // Handle static control buttons
        switch (clickedSlot) {
            case GuiConstants.SHOP_CLOSE_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 0.8f);
                return;

            case GuiConstants.SHOP_PLAYER_HEAD_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 0.8f, 0.8f);
                effectsCommand.OpenOwnedEffects(player);
                return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        String effectKey = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "effect_key"),
                PersistentDataType.STRING
        );
        if (effectKey == null) return;

        String permission = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "effect_permission"),
                PersistentDataType.STRING
        );
        if (permission == null) return;

        boolean purchaseSuccess = plugin.getEffectsHandler().handlePurchase(player, effectKey, permission);
        if (purchaseSuccess)
            shopCommand.OpenShop(player);
    }
}