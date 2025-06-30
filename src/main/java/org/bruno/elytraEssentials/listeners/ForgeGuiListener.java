package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.commands.ForgeCommand;
import org.bruno.elytraEssentials.helpers.GuiHelper;
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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
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

    private final NamespacedKey armoredElytraKey;
    private final NamespacedKey materialKey;
    private final NamespacedKey previewItemKey;

    public ForgeGuiListener(ElytraEssentials plugin, ForgeCommand forgeCommand) {
        this.plugin = plugin;
        this.forgeCommand = forgeCommand;

        this.armoredElytraKey = new NamespacedKey(plugin, Constants.NBT.ARMORED_ELYTRA_TAG);
        this.materialKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_MATERIAL_TAG);
        this.previewItemKey = new NamespacedKey(plugin, Constants.NBT.PREVIEW_ITEM_TAG);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled()) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() == null || !(topInventory.getHolder() instanceof ForgeHolder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // We schedule an update after every click to keep the GUI state correct.
        // The one-tick delay allows Bukkit to process the click action first.
        scheduleResultUpdate(topInventory);

        Inventory clickedInventory = event.getClickedInventory();

        // Handle Shift-Clicks from the Player's Inventory
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (clickedInventory != null && clickedInventory.equals(player.getInventory())) {
                event.setCancelled(true);
                handleShiftClick(event.getCurrentItem(), topInventory);
                return;
            }
        }

        if (topInventory.equals(clickedInventory)) {
            int slot = event.getSlot();
            ItemStack clickedItem = event.getCurrentItem();

            switch (slot) {
                case Constants.GUI.FORGE_CONFIRM_SLOT:
                    event.setCancelled(true);

                    //  Only handle click if the confirm button is actually there
                    if (clickedItem != null && clickedItem.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                        handleConfirmClick(topInventory, player);
                    }
                    break;
                case Constants.GUI.FORGE_CANCEL_SLOT:

                    event.setCancelled(true);
                    if (clickedItem != null && clickedItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                        player.closeInventory();
                    }
                    break;
                default:
                    event.setCancelled(true);
                    break;
            }
        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof ForgeHolder)) return;

        Player player = (Player) event.getPlayer();
        Inventory forge = event.getInventory();

        // Return only the non-preview items to the player.
        returnItemToPlayer(player, forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT));
        returnItemToPlayer(player, forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT));
        returnItemToPlayer(player, forge.getItem(Constants.GUI.FORGE_RESULT_SLOT));

        if (forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT) != null
                ||  forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT) != null
                    || forge.getItem(Constants.GUI.FORGE_RESULT_SLOT) != null)
        {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 0.8f);
        }
    }


    private void handleConfirmClick(Inventory forge, Player player) {
        ItemStack resultItem = forge.getItem(Constants.GUI.FORGE_RESULT_SLOT);
        ItemStack revertedElytra = forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT);
        ItemStack revertedArmor = forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT);

        if (isPreviewItem(resultItem)) {
            // Crafting
            returnItemToPlayer(player, createCleanCopy(resultItem));
            forge.clear();
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.closeInventory();
        }
        else if (isPreviewItem(revertedElytra) && isPreviewItem(revertedArmor)) {
            // Reverting
            returnItemToPlayer(player, createCleanCopy(revertedElytra));
            returnItemToPlayer(player, createCleanCopy(revertedArmor));
            forge.clear();
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.0f);
            player.closeInventory();
        }
    }

    private void returnItemToPlayer(Player player, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && !isPreviewItem(item)) {
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            if (!leftovers.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }
    }


    private void handleShiftClick(ItemStack clickedItem, Inventory forge) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemStack singleItem = clickedItem.clone();
        singleItem.setAmount(1);

        if (isPlainElytra(clickedItem) && isEmpty(forge.getItem(Constants.GUI.FORGE_ELYTRA_SLOT))) {
            forge.setItem(Constants.GUI.FORGE_ELYTRA_SLOT, singleItem);
            clickedItem.setAmount(clickedItem.getAmount() - 1);
        } else if (isChestplate(clickedItem) && isEmpty(forge.getItem(Constants.GUI.FORGE_ARMOR_SLOT))) {
            forge.setItem(Constants.GUI.FORGE_ARMOR_SLOT, singleItem);
            clickedItem.setAmount(clickedItem.getAmount() - 1);
        } else if (isArmoredElytra(clickedItem) && isEmpty(forge.getItem(Constants.GUI.FORGE_RESULT_SLOT))) {
            forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, singleItem);
            clickedItem.setAmount(clickedItem.getAmount() - 1);
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
        ItemStack middleSlot = forge.getItem(Constants.GUI.FORGE_RESULT_SLOT);

        if (isArmoredElytra(middleSlot) && !isPreviewItem(middleSlot)) {
            displayRevertedItems(forge, middleSlot);
            showActionButtons(forge, "Revert");
        } else if (isPlainElytra(elytraSlot) && isChestplate(armorSlot) && !isPreviewItem(elytraSlot) && !isPreviewItem(armorSlot)) {
            ItemStack armoredElytra = createArmoredElytra(elytraSlot, armorSlot);
            forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, armoredElytra);
            showActionButtons(forge, "Forge");
        }
        else {
            clearActionButtons(forge);
        }
    }

    private void showActionButtons(Inventory forge, String action) {
        ItemStack confirmButton = GuiHelper.createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§aConfirm " + action, "§7Click to complete the process.");
        ItemStack cancelButton = GuiHelper.createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cCancel", "§7Click to close the menu.");
        forge.setItem(Constants.GUI.FORGE_CONFIRM_SLOT, confirmButton);
        forge.setItem(Constants.GUI.FORGE_CANCEL_SLOT, cancelButton);
    }

    private void clearActionButtons(Inventory forge) {
        forge.setItem(Constants.GUI.FORGE_CONFIRM_SLOT, null);
        forge.setItem(Constants.GUI.FORGE_CANCEL_SLOT, null);
    }

    private void displayRevertedItems(Inventory forge, ItemStack armoredElytra) {
        ItemStack plainElytra = reassembleElytra(armoredElytra);
        ItemStack chestplate = reassembleChestplate(armoredElytra);

        //  handle armored elytra durability
        if (armoredElytra.getItemMeta() instanceof Damageable sourceDamage) {
            ItemMeta plainMeta = plainElytra.getItemMeta();
            if (plainMeta instanceof Damageable targetDamage) {
                targetDamage.setDamage(sourceDamage.getDamage());
                plainElytra.setItemMeta(targetDamage);
            }
        }

        // Tag them as preview items
        ItemMeta metaPlainElytra = plainElytra.getItemMeta();
        if (metaPlainElytra != null){
            metaPlainElytra.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.PREVIEW_ITEM_TAG), PersistentDataType.BOOLEAN, true);
            plainElytra.setItemMeta(metaPlainElytra);
        }

        forge.setItem(Constants.GUI.FORGE_ELYTRA_SLOT, plainElytra);
        forge.setItem(Constants.GUI.FORGE_ARMOR_SLOT, chestplate);
    }

    private ItemStack reassembleChestplate(ItemStack armoredElytra) {
        ItemMeta sourceMeta = armoredElytra.getItemMeta();
        if (sourceMeta == null) return null;

        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        String materialName = container.get(materialKey, PersistentDataType.STRING);
        Material armorType = (materialName != null) ? Material.matchMaterial(materialName) : Material.DIAMOND_CHESTPLATE;
        ItemStack chestplate = new ItemStack(armorType);
        ItemMeta chestMeta = chestplate.getItemMeta();

        if (chestMeta != null) {
            // Loop through all enchantments and look for ones stored with the "chestplate_" prefix
            for (Enchantment enchantment : Enchantment.values()) {
                NamespacedKey key = new NamespacedKey(plugin, "chestplate_enchant_" + enchantment.getKey().getKey());
                if (container.has(key, PersistentDataType.INTEGER)) {
                    int level = container.get(key, PersistentDataType.INTEGER);
                    chestMeta.addEnchant(enchantment, level, true);
                }
            }
            chestMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.PREVIEW_ITEM_TAG), PersistentDataType.BOOLEAN, true);

            chestplate.setItemMeta(chestMeta);
        }
        return chestplate;
    }

    private ItemStack reassembleElytra(ItemStack armoredElytra) {
        ItemMeta sourceMeta = armoredElytra.getItemMeta();
        if (sourceMeta == null) return null;

        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();
        ItemStack plainElytra = new ItemStack(Material.ELYTRA);
        ItemMeta elytraMeta = plainElytra.getItemMeta();

        if (elytraMeta instanceof Damageable targetDamage) {
            if (sourceMeta instanceof Damageable sourceDamage) {
                targetDamage.setDamage(sourceDamage.getDamage());
            }
        }

        if (elytraMeta != null) {
            // Loop through all enchantments and look for ones stored with the "elytra_" prefix
            for (Enchantment enchantment : Enchantment.values()) {
                NamespacedKey key = new NamespacedKey(plugin, "elytra_enchant_" + enchantment.getKey().getKey());
                if (container.has(key, PersistentDataType.INTEGER)) {
                    int level = container.get(key, PersistentDataType.INTEGER);
                    elytraMeta.addEnchant(enchantment, level, true);
                }
            }
            elytraMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.PREVIEW_ITEM_TAG), PersistentDataType.BOOLEAN, true);

            plainElytra.setItemMeta(elytraMeta);
        }
        return plainElytra;
    }

    //  ############### HELPER METHODS #######################


    private ItemStack createCleanCopy(ItemStack item) {
        if (item == null) return null;
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(previewItemKey);
            copy.setItemMeta(meta);
        }
        return copy;
    }

    private boolean isArmoredElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(armoredElytraKey, PersistentDataType.BOOLEAN);
    }

    private boolean isPreviewItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(previewItemKey, PersistentDataType.BOOLEAN);
    }

    private boolean isChestplate(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("_CHESTPLATE");
    }

    private boolean isPlainElytra(ItemStack item) {
        return item != null && item.getType() == Material.ELYTRA && !isArmoredElytra(item);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private ItemStack createArmoredElytra(ItemStack elytra, ItemStack chestplate) {
        ItemStack armoredElytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = armoredElytra.getItemMeta();

        //  handle chestplate and elytra durability
        if (elytra.getItemMeta() instanceof Damageable sourceDamage && meta instanceof Damageable targetDamage) {
            targetDamage.setDamage(sourceDamage.getDamage());
        }

        Material armorType = chestplate.getType();
        int maxArmorDurability = armorType.getMaxDurability();
        int currentArmorDamage = 0;
        if (chestplate.getItemMeta() instanceof Damageable sourceArmorDamage) {
            currentArmorDamage = sourceArmorDamage.getDamage();
        }
        int currentArmorDurability = maxArmorDurability - currentArmorDamage;

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

            double percentage = (maxArmorDurability > 0) ? (double) currentArmorDurability / maxArmorDurability : 0;
            String durabilityColor;
            if (percentage == 1){
                durabilityColor = "§a";
            } else if (percentage > 0.5) {
                durabilityColor = "§e";
            } else if (percentage > 0.2) {
                durabilityColor = "§c";
            } else {
                durabilityColor = "§4";
            }
            lore.add(String.format("§6Armor Plating: %s%d §7/ §a%d", durabilityColor, currentArmorDurability, maxArmorDurability));

            //  Get enchantments from both input items.
            Map<Enchantment, Integer> elytraEnchants = elytra.getEnchantments();
            Map<Enchantment, Integer> chestplateEnchants = chestplate.getEnchantments();

            //  Create a map for display purposes, merging the two lists
            //  and keeping the higher level of any duplicate enchantments.
            Map<Enchantment, Integer> displayEnchants = new HashMap<>(chestplateEnchants);
            elytraEnchants.forEach((enchant, level) ->
                    displayEnchants.merge(enchant, level, Integer::max)
            );

            //  Build the lore using the clean, merged display map.
            if (!displayEnchants.isEmpty()) {
                lore.add("");
                lore.add("§dEnchantments:");
            }
            for (Map.Entry<Enchantment, Integer> entry : displayEnchants.entrySet()) {
                lore.add(String.format(" §f- §7%s %d", getEnchantmentName(entry.getKey()), entry.getValue()));
            }

            meta.setLore(lore);

            //  Save the original enchantments to NBT with prefixes to preserve their source.
            //  This is crucial for the revert process to work correctly.
            for (Map.Entry<Enchantment, Integer> entry : elytraEnchants.entrySet()) {
                NamespacedKey key = new NamespacedKey(plugin, "elytra_enchant_" + entry.getKey().getKey().getKey());
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, entry.getValue());
            }
            for (Map.Entry<Enchantment, Integer> entry : chestplateEnchants.entrySet()) {
                NamespacedKey key = new NamespacedKey(plugin, "chestplate_enchant_" + entry.getKey().getKey().getKey());
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, entry.getValue());
            }

            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.ARMORED_ELYTRA_TAG), PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG), PersistentDataType.INTEGER, currentArmorDurability);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.MAX_ARMOR_DURABILITY_TAG), PersistentDataType.INTEGER, maxArmorDurability);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.ARMOR_MATERIAL_TAG), PersistentDataType.STRING, chestplate.getType().name());
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.PREVIEW_ITEM_TAG), PersistentDataType.BOOLEAN, true);

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
