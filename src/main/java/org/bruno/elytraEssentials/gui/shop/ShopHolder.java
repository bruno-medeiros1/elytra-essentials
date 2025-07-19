package org.bruno.elytraEssentials.gui.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ShopHolder implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null; // Not used, but required to implement
    }
}