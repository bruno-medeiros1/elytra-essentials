package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.commands.ForgeCommand;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.gui.ForgeHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//  Handle player interactions within the forge menu.
public class ForgeGuiListener implements Listener {

    private final ElytraEssentials plugin;
    private final ForgeCommand forgeCommand;

    public ForgeGuiListener(ElytraEssentials plugin, ForgeCommand forgeCommand) {
        this.plugin = plugin;
        this.forgeCommand = forgeCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled()){
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        // Check if the opened inventory is our forge
        if (topInventory.getHolder() == null || !(topInventory.getHolder() instanceof ForgeHolder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        //  Handle Clicks within the Forge GUI
        if (clickedInventory != null && clickedInventory.getHolder() instanceof ForgeHolder) {
            // Allow them to take the result if it exists
            if (event.getSlot() == Constants.GUI.FORGE_RESULT_SLOT) {
                handleResultClick(event, player);
            } else {
                // Prevent taking items from any other slot (inputs, panes)
                event.setCancelled(true);
            }

        } else if (clickedInventory != null) {
            // Player is clicking in their own inventory
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // This is a shift-click. Cancel it and handle it manually.
                event.setCancelled(true);
                handleShiftClick(event.getCurrentItem(), topInventory);
            }
        }

        // Schedule an update to the result slot after the event has been processed
        scheduleResultUpdate(topInventory);
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled()){
            return;
        }

        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof ForgeHolder)) {
            return;
        }

        Inventory forge = event.getInventory();
        Player player = (Player) event.getPlayer();

        ItemStack elytraItem = forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT);
        ItemStack armorItem = forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT);

        if (elytraItem != null) {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(elytraItem);
            if (!leftovers.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), elytraItem);
                player.sendMessage("§cYour inventory was full, so the Elytra was dropped on the ground!");
            }
        }
        if (armorItem != null) {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(armorItem);
            if (!leftovers.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), armorItem);
                player.sendMessage("§cYour inventory was full, so the Chestplate was dropped on the ground!");
            }
        }
    }

    private void handleResultClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack resultItem = event.getCurrentItem();

        if (resultItem == null || resultItem.getType() == Material.AIR) {
            return;
        }

        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(resultItem.clone());

        if (!leftovers.isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), resultItem);
            player.sendMessage("§cYour inventory was full, so the Armored Elytra was dropped on the ground!");
        }

        // Whether the item was given or dropped, the craft was successful.
        event.getInventory().setItem(Constants.GUI.FORGE_ELYTRA_SLOT, null);
        event.getInventory().setItem(Constants.GUI.FORGE_ARMOR_SLOT, null);
        event.getInventory().setItem(Constants.GUI.FORGE_RESULT_SLOT, null);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
    }

    private void handleShiftClick(ItemStack clickedItem, Inventory forge) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (clickedItem.getType() == Material.ELYTRA) {
            // Try to move it to the elytra slot
            ItemStack elytraSlot = forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT);
            if (elytraSlot == null || elytraSlot.getType() == Material.AIR) {
                forge.setItem(Constants.GUI.FORGE_ELYTRA_SLOT, clickedItem.clone());
                clickedItem.setAmount(0); // Remove from player's inventory
            }
        } else if (isChestplate(clickedItem)) {
            // Try to move it to the armor slot
            ItemStack armorSlot = forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT);
            if (armorSlot == null || armorSlot.getType() == Material.AIR) {
                forge.setItem(Constants.GUI.FORGE_ARMOR_SLOT, clickedItem.clone());
                clickedItem.setAmount(0); // Remove from player's inventory
            }
        }
    }

    private void scheduleResultUpdate(Inventory forge) {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateForgeResult(forge);
            }
        }.runTask(plugin);
    }

    private void updateForgeResult(Inventory forge) {
        ItemStack elytraSlot = forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT);
        ItemStack armorSlot = forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT);

        if (elytraSlot != null && elytraSlot.getType() == Material.ELYTRA &&
                isChestplate(armorSlot)) {
            ItemStack armoredElytra = createArmoredElytra(elytraSlot, armorSlot);
            forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, armoredElytra);
        } else {
            forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, null);
        }
    }

    private boolean isChestplate(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("_CHESTPLATE");
    }

    private ItemStack createArmoredElytra(ItemStack elytra, ItemStack chestplate) {
        ItemStack armoredElytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = armoredElytra.getItemMeta();
        Material armorType = chestplate.getType();
        int maxDurability = armorType.getMaxDurability();

        int armorPoints = 0;
        int armorToughness = 0;
        switch (armorType) {
            case LEATHER_CHESTPLATE: armorPoints = 3; break;
            case CHAINMAIL_CHESTPLATE: armorPoints = 5; break;
            case IRON_CHESTPLATE: armorPoints = 6; break;
            case GOLDEN_CHESTPLATE: armorPoints = 5; break;
            case DIAMOND_CHESTPLATE: armorPoints = 8; armorToughness = 2; break;
            case NETHERITE_CHESTPLATE: armorPoints = 8; armorToughness = 3; break;
        }

        if (meta != null) {
            meta.setDisplayName("§b§lArmored Elytra");

            List<String> lore = new ArrayList<>();
            lore.add("§7A fusion of flight and protection.");
            lore.add("");
            lore.add("§6Armor Stats:");
            lore.add(String.format(" §f- §7Armor: §a+%d", armorPoints));
            if (armorToughness > 0) {
                lore.add(String.format(" §f- §7Armor Toughness: §a+%d", armorToughness));
            }
            lore.add("");
            lore.add(String.format("§6Armor Plating: §f%d / §c%d", maxDurability, maxDurability));

            //  Get the enchantments from the input chestplate.
            Map<Enchantment, Integer> enchantments = chestplate.getEnchantments();
            if (!enchantments.isEmpty()) {
                lore.add("");
                lore.add("§dEnchantments:");
            }

            //  Loop through each enchantment and store it in the item's NBT.
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = entry.getKey();
                int level = entry.getValue();

                // Create a unique key for each enchantment, e.g., "enchant_protection"
                NamespacedKey key = new NamespacedKey(plugin, "enchant_" + enchantment.getKey().getKey());

                // Store the enchantment level in the Persistent Data Container.
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);

                // Add the enchantment to the lore to show the player it was transferred.
                lore.add(String.format(" §f- §7%s %d", getEnchantmentName(enchantment), level));
            }

            meta.setLore(lore);

            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.ARMORED_ELYTRA_TAG), PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG), PersistentDataType.INTEGER, maxDurability);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.MAX_ARMOR_DURABILITY_TAG), PersistentDataType.INTEGER, maxDurability);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.ARMOR_MATERIAL_TAG), PersistentDataType.STRING, chestplate.getType().name());

            armoredElytra.setItemMeta(meta);
        }
        return armoredElytra;
    }

    private String getEnchantmentName(Enchantment enchantment) {
        String name = enchantment.getKey().getKey().replace("_", " ");
        // Capitalize the first letter of each word
        String[] words = name.split(" ");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            capitalized.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return capitalized.toString().trim();
    }
}
