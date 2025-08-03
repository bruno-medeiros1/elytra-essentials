package org.bruno.elytraEssentials.gui.upgrade;

import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ArmoredElytraHandler;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.helpers.*;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.UpgradeType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class UpgradeGuiHandler {
    private final ElytraEssentials plugin;
    private final ArmoredElytraHelper armoredElytraHelper;
    private final ArmoredElytraHandler armoredElytraHandler;
    private final ConfigHandler configHandler;
    private final MessagesHelper messagesHelper;
    private final Economy economy;

    public UpgradeGuiHandler(ElytraEssentials plugin, ArmoredElytraHelper armoredElytraHelper, ConfigHandler configHandler,
                             MessagesHelper messagesHelper, Economy economy, ArmoredElytraHandler armoredElytraHandler) {
        this.plugin = plugin;
        this.armoredElytraHelper = armoredElytraHelper;
        this.armoredElytraHandler = armoredElytraHandler;
        this.configHandler = configHandler;
        this.messagesHelper = messagesHelper;
        this.economy = economy;
    }

    /**
     * Creates and opens the upgrade GUI for a player.
     */
    public void open(Player player) {
        ItemStack elytra = player.getInventory().getChestplate();
        if (elytra == null || !armoredElytraHelper.isArmoredElytra(elytra)) {
            messagesHelper.sendPlayerMessage(player, "&cError: No Armored Elytra equipped.");
            return;
        }

        Inventory gui = Bukkit.createInventory(new UpgradeHolder(), Constants.GUI.UPGRADE_INVENTORY_SIZE, "Elytra Upgrade Station");

        // Create border items
        ItemStack blackPane = GuiHelper.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Fill the entire border
        for (int i = 0; i < gui.getSize(); i++) {
            if (i < 10 || i > 34 || i % 9 == 0 || (i + 1) % 9 == 0) {
                gui.setItem(i, blackPane);
            }
        }

        gui.setItem(4, blackPane);
        gui.setItem(38, blackPane);
        gui.setItem(4, elytra.clone());

        // Place the upgrade items
        populateUpgradeItems(gui, elytra);

        // Place informational and action items
        if (economy != null) {
            gui.setItem(36, GuiHelper.createPlayerHead(player, "§6Your Balance", "§e$" + String.format("%,.2f", economy.getBalance(player))));
        }
        gui.setItem(Constants.GUI.UPGRADE_CLOSE_SLOT, GuiHelper.createCloseButton());

        player.openInventory(gui);
    }

    /**
     * Populates the GUI with items representing each possible upgrade by iterating
     * through the UpgradeType enum.
     */
    private void populateUpgradeItems(Inventory gui, ItemStack elytra) {
        ItemMeta meta = elytra.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();

        int slot = 19; // Starting slot for upgrades
        for (UpgradeType type : UpgradeType.values()) {

            // Special case: Only show the efficiency upgrade if the time limit is enabled.
            if (type == UpgradeType.FLIGHT_EFFICIENCY && !configHandler.getIsTimeLimitEnabled()) {
                continue;
            }

            // Special case: Only show the kinetic resistance upgrade if the kinetic energy protection isn't enabled.
            if (type == UpgradeType.KINETIC_RESISTANCE && configHandler.getIsKineticEnergyProtectionEnabled()) {
                continue;
            }

            NamespacedKey key = new NamespacedKey(plugin, type.getKey());
            int currentLevel = container.getOrDefault(key, PersistentDataType.INTEGER, 0);

            gui.setItem(slot, createUpgradeItem(type, currentLevel));
            slot++;
        }
    }

    /**
     * Creates the ItemStack for a single upgrade option using data from the UpgradeType enum.
     */
    private ItemStack createUpgradeItem(UpgradeType type, int currentLevel) {
        boolean isMaxLevel = currentLevel >= type.getMaxLevel();
        Material material = isMaxLevel ? Material.REDSTONE_BLOCK : type.getDisplayMaterial();

        List<String> lore = new ArrayList<>();

        // Description
        String[] descriptionLines = type.getDescription().split("\n");
        for (String line : descriptionLines) {
            lore.add("§7" + line);
        }
        lore.add("");
        lore.add("§8§m--------------------");

        // Current Stats
        lore.add("§7Level: §f" + currentLevel + " §7/ §f" + type.getMaxLevel());
        if (currentLevel > 0) {
            double currentBonus = currentLevel * type.getValuePerLevel();
            String currentBonusString = String.format("%.1f", currentBonus).replace(".0", "");
            lore.add(ColorHelper.parse("&7Bonus: &#FFBFBF+" + currentBonusString + type.getSuffix()));
        }
        lore.add("");

        if (isMaxLevel) {
            lore.add("§c§lMAX LEVEL REACHED");
            lore.add("§8§m--------------------");
        } else {
            // Next Upgrade Info
            lore.add("§cNext Upgrade:");
            double nextBonus = (currentLevel + 1) * type.getValuePerLevel();
            String nextBonusString = String.format("%.1f", nextBonus).replace(".0", "");
            lore.add(ColorHelper.parse(" &f▪ &#FFBFBFLevel: " + (currentLevel + 1) + " &7(&4+" + nextBonusString + type.getSuffix() + "&7)"));

            double cost = 1000 * (currentLevel + 1); // Placeholder for a configurable cost
            lore.add(ColorHelper.parse(" §f▪ &#FFBFBFCost: §c$" + String.format("%,.0f", cost)));
            lore.add("§8§m--------------------");
            lore.add("");
            lore.add("§aClick to Upgrade");
        }

        ItemStack item = GuiHelper.createGuiItem(material, ColorHelper.parse("&#FFBFBF" + type.getDisplayName()), lore.toArray(new String[0]));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "upgrade_type"), PersistentDataType.STRING, type.name());
            if (isMaxLevel) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Handles all click events within the upgrade GUI.
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if (clickedMeta == null) return;

        int clickedSlot = event.getSlot();

        if (clickedSlot == Constants.GUI.UPGRADE_CLOSE_SLOT) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
            return;
        }

        String upgradeTypeName = clickedMeta.getPersistentDataContainer().get(new NamespacedKey(plugin, "upgrade_type"), PersistentDataType.STRING);
        if (upgradeTypeName == null) return; // Not an upgrade item

        UpgradeType upgradeType = UpgradeType.valueOf(upgradeTypeName);

        // Purchase Logic
        ItemStack elytra = player.getInventory().getChestplate();
        if (elytra == null || !armoredElytraHelper.isArmoredElytra(elytra)) {
            player.closeInventory();
            messagesHelper.sendPlayerMessage(player, "&cYou must be wearing an Armored Elytra.");
            return;
        }

        ItemMeta elytraMeta = elytra.getItemMeta();
        if (elytraMeta == null) return;

        PersistentDataContainer container = elytraMeta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, upgradeType.getKey());
        int currentLevel = container.getOrDefault(key, PersistentDataType.INTEGER, 0);

        if (currentLevel >= upgradeType.getMaxLevel()) {
            messagesHelper.sendPlayerMessage(player, "&cThis upgrade is already at its maximum level.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        double cost = 1000 * (currentLevel + 1);
        // If an economy is present, check the player's balance and withdraw the cost.
        if (economy != null) {
            if (economy.getBalance(player) < cost) {
                messagesHelper.sendPlayerMessage(player, "&cYou don't have enough money for this upgrade.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            economy.withdrawPlayer(player, cost);
        } else {
            plugin.getLogger().warning("No Vault economy provider found. Elytra upgrades will be free.");
        }

        // Update NBT data for the upgrade
        container.set(key, PersistentDataType.INTEGER, currentLevel + 1);
        elytra.setItemMeta(elytraMeta);
        updateElytraLore(elytra);

        // Immediately apply the new upgrades to the elytra after the purchase if applicable
        if (upgradeType == UpgradeType.ARMOR_DURABILITY || upgradeType == UpgradeType.ARMOR_PROTECTION || upgradeType == UpgradeType.ARMOR_TOUGHNESS) {
            armoredElytraHandler.removeArmorAttributes(player);
            armoredElytraHandler.applyUpgradedAttributes(player, elytra);
        }

        // Finally, set the fully modified item back to the player's inventory.
        player.getInventory().setChestplate(elytra);


        player.getInventory().setChestplate(elytra);

        messagesHelper.sendPlayerMessage(player, "&aSuccessfully upgraded " + upgradeType.getDisplayName() + " to Level " + (currentLevel + 1) + "!");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);

        // Refresh the GUI to show the new level
        open(player);
    }

    /**
     * Rebuilds the lore of the elytra to include the new upgrade information.
     * This method now takes the ItemStack directly to ensure it reads the latest data.
     */
    private void updateElytraLore(ItemStack elytra) {
        ItemMeta elytraMeta = elytra.getItemMeta();
        if (elytraMeta == null) return;

        PersistentDataContainer container = elytraMeta.getPersistentDataContainer();

        List<String> baseLore = new ArrayList<>();
        if (elytraMeta.getLore() != null) {
            for (String line : elytraMeta.getLore()) {
                if (line.contains("Upgrades:")) {
                    break;
                }
                baseLore.add(line);
            }
        }

        // Remove any trailing empty lines
        while (!baseLore.isEmpty() && baseLore.get(baseLore.size() - 1).trim().isEmpty()) {
            baseLore.remove(baseLore.size() - 1);
        }

        List<String> upgradeLines = new ArrayList<>();
        for (UpgradeType type : UpgradeType.values()) {
            NamespacedKey key = new NamespacedKey(plugin, type.getKey());
            int level = container.getOrDefault(key, PersistentDataType.INTEGER, 0);
            if (level > 0) {
                double bonus = level * type.getValuePerLevel();
                String bonusString = String.format("%.1f", bonus).replace(".0", "");
                upgradeLines.add(ColorHelper.parse(" &f▪ &7" + type.getDisplayName() + ": &#FFBFBFLevel " + level + " &7(&4+" + bonusString + type.getSuffix() + "&7)"));
            }
        }

        if (!upgradeLines.isEmpty()) {
            baseLore.add("");
            baseLore.add(ColorHelper.parse("&cUpgrades:"));
            baseLore.addAll(upgradeLines);
        }

        elytraMeta.setLore(baseLore);
        elytra.setItemMeta(elytraMeta);
    }
}