package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.commands.EffectsCommand;
import org.bruno.elytraEssentials.commands.ShopCommand;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.gui.ShopHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopGuiListener implements Listener {
    private final ElytraEssentials plugin;
    private final EffectsCommand effectsCommand;
    private final ShopCommand shopCommand;

    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public ShopGuiListener(ElytraEssentials plugin, EffectsCommand effectsCommand, ShopCommand shopCommand) {
        this.plugin = plugin;
        this.effectsCommand = effectsCommand;
        this.shopCommand = shopCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof ShopHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
        int clickedSlot = event.getSlot();

        switch (clickedSlot) {
            case Constants.GUI.SHOP_PREVIOUS_PAGE_SLOT:
                if (currentPage > 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
                    shopCommand.OpenShop(player, currentPage - 1);
                }
                break;
            case Constants.GUI.SHOP_NEXT_PAGE_SLOT:
                int totalItems = plugin.getEffectsHandler().getEffectsRegistry().size();
                int totalPages = (int) Math.ceil((double) totalItems / Constants.GUI.SHOP_ITEMS_PER_PAGE);
                if (currentPage < totalPages - 1) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
                    shopCommand.OpenShop(player, currentPage + 1);
                }
                break;
            case Constants.GUI.SHOP_CLOSE_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 0.8f);
                break;
            case Constants.GUI.SHOP_PLAYER_HEAD_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 0.8f, 0.8f);
                effectsCommand.OpenOwnedEffects(player);
                break;
            default:
                // Handle clicks on effect items for purchasing
                handlePurchaseClick(player, clickedItem, currentPage);
                break;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerPages.remove(event.getPlayer().getUniqueId());
    }

    public void setCurrentPage(UUID uuid, int page) {
        playerPages.put(uuid, page);
    }

    private void handlePurchaseClick(Player player, ItemStack clickedItem, int currentPage) {
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

        boolean purchaseSuccess = plugin.getEffectsHandler().handlePurchase(player, effectKey, permission);

        if (purchaseSuccess) {
            shopCommand.OpenShop(player, currentPage);
        }
    }
}