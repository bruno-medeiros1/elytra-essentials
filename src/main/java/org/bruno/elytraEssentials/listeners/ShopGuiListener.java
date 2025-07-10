package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
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

    // We need to track which page each player is currently viewing.
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public ShopGuiListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    public void setCurrentPage(UUID uuid, int page) {
        playerPages.put(uuid, page);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up the map when a player leaves to prevent memory leaks.
        playerPages.remove(event.getPlayer().getUniqueId());
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

                    playerPages.put(player.getUniqueId(), currentPage - 1);

                    plugin.openShopGUI(player, currentPage - 1);
                }
                break;
            case Constants.GUI.SHOP_NEXT_PAGE_SLOT:
                int totalItems = plugin.getEffectsHandler().getEffectsRegistry().size();
                int totalPages = (int) Math.ceil((double) totalItems / Constants.GUI.SHOP_ITEMS_PER_PAGE);
                if (currentPage < totalPages - 1) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);

                    playerPages.put(player.getUniqueId(), currentPage + 1);

                    plugin.openShopGUI(player, currentPage + 1);
                }
                break;
            case Constants.GUI.SHOP_CLOSE_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 0.8f);
                break;
            case Constants.GUI.SHOP_PLAYER_HEAD_SLOT:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 0.8f, 0.8f);

                plugin.openEffectsGUI(player);
                break;
            default:
                handlePurchaseClick(player, clickedItem, currentPage);
                break;
        }
    }

    private void handlePurchaseClick(Player player, ItemStack clickedItem, int currentPage) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        String effectKey = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, Constants.NBT.EFFECT_KEY),
                PersistentDataType.STRING
        );
        if (effectKey == null) return;

        String permission = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, Constants.NBT.EFFECT_PERMISSION_KEY),
                PersistentDataType.STRING
        );

        boolean purchaseSuccess = plugin.getEffectsHandler().handlePurchase(player, effectKey, permission);

        // If the purchase was successful, refresh the GUI to update the item's lore
        if (purchaseSuccess) {
            plugin.openShopGUI(player, currentPage);
        }
    }
}