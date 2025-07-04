package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArmorCommand implements ISubCommand {
    private final ElytraEssentials plugin;

    public ArmorCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    ///
    /// TODO: Adicionar Item History
    /// ...
    /// §eItem History:
    /// §f - §7Total Damage Absorbed: §a1,240
    /// §f - §7Times Plating Shattered: §c2
    /// §f - §7Forged By: §dbruno_medeiros1
    /// ...

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            return true;

        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled()){
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getFeatureNotEnabled());
            return true;
        }

        if (!PermissionsHelper.hasArmorPermission(player)){
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return true;
        }

        ItemStack chestplate = player.getInventory().getChestplate();

        // Check if the player is wearing an Armored Elytra
        if (!isArmoredElytra(chestplate)) {
            player.sendMessage(ChatColor.RED + "You are not currently wearing an Armored Elytra.");
            return true;
        }

        // --- Get all the data from the item ---
        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "Error: Could not read item data.");
            return true;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int currentDurability = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG), PersistentDataType.INTEGER, 0);
        int maxDurability = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.MAX_ARMOR_DURABILITY_TAG), PersistentDataType.INTEGER, 1);
        String materialName = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.ARMOR_MATERIAL_TAG), PersistentDataType.STRING, "Unknown");
        materialName = materialName.replace("_CHESTPLATE", "").replace("_", " "); // Format for display

        //  Get Item History Data
        double damageAbsorbed = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.DAMAGE_ABSORBED_TAG), PersistentDataType.DOUBLE, 0.0);
        int timesShattered = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.PLATING_SHATTERED_TAG), PersistentDataType.INTEGER, 0);
        String forgedBy = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.FORGED_BY_TAG), PersistentDataType.STRING, "§kUnknown");

        //  Formatting
        ChatColor primary = ChatColor.AQUA;
        ChatColor secondary = ChatColor.DARK_AQUA;
        ChatColor text = ChatColor.GRAY;
        ChatColor value = ChatColor.WHITE;
        String arrow = "» ";

        //  Build and send the message ---
        player.sendMessage(primary + "§m----------------------------------------------------");
        player.sendMessage("");
        player.sendMessage(primary + "§lArmored Elytra Info");
        player.sendMessage("");

        // Display Armor Plating Durability
        player.sendMessage("§3» Armor Plating");
        player.sendMessage(createDurabilityBar(currentDurability, maxDurability));
        player.sendMessage("");
        player.sendMessage(secondary + arrow + text + "Base Material: " + value + getCapitalizedName(materialName));

        //  Display Stored Enchantments
        List<String> enchantmentLines = getEnchantmentLines(container);
        if (!enchantmentLines.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(secondary + "Stored Enchantments:");
            for (String line : enchantmentLines) {
                player.sendMessage(line);
            }
        }

        //  Display Item History
        player.sendMessage("");
        player.sendMessage(secondary + "Item History:");

        double damageInHearts = damageAbsorbed / 2.0;
        player.sendMessage(String.format("%s" + arrow + "%sTotal Damage Absorbed: %s%.1f ❤", secondary, text, "§a", damageInHearts));
        player.sendMessage(String.format("%s" + arrow + "%sTimes Plating Shattered: %s%d", secondary, text, "§c", timesShattered));
        player.sendMessage(String.format("%s" + arrow + "%sForged By: %s%s", secondary, text, "§f", forgedBy));

        player.sendMessage("");
        player.sendMessage(primary + "§m----------------------------------------------------");

        return true;
    }

    private String createDurabilityBar(int current, int max) {
        // Ensure we don't divide by zero if max durability is 0 for some reason
        double percentage = (max > 0) ? (double) current / max : 0;

        // Determine the color based on the percentage
        String barColor;
        if (percentage == 1) {
            barColor = "§a"; // Full health
        } else if (percentage > 0.5) {
            barColor = "§e"; // Medium health
        } else if (percentage > 0.2) {
            barColor = "§c"; // Low health
        } else {
            barColor = "§4"; // Critically low health
        }

        int totalSegments = 20; // The total width of the bar in characters
        int filledSegments = (int) (totalSegments * percentage);

        // Build the string for the bar itself
        StringBuilder bar = new StringBuilder();
        bar.append(barColor);
        for (int i = 0; i < totalSegments; i++) {
            if (i < filledSegments) {
                bar.append("▆"); // Filled segment
            } else {
                // Use a gray color for the empty part of the bar
                bar.append("§f▆");
            }
        }

        double displayPercentage = percentage * 100.0;
        bar.append(String.format(" %s§l%.2f%%", barColor, displayPercentage));

        return bar.toString();
    }

    private boolean isArmoredElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(new NamespacedKey(plugin, Constants.NBT.ARMORED_ELYTRA_TAG), PersistentDataType.BOOLEAN);
    }

    private List<String> getEnchantmentLines(PersistentDataContainer container) {
        List<String> lines = new ArrayList<>();
        Map<Enchantment, Integer> displayEnchants = new HashMap<>();

        // Loop through all possible enchantments
        for (Enchantment enchantment : Enchantment.values()) {
            // Create keys for both potential sources
            NamespacedKey elytraEnchantKey = new NamespacedKey(plugin, "elytra_enchant_" + enchantment.getKey().getKey());
            NamespacedKey chestplateEnchantKey = new NamespacedKey(plugin, "chestplate_enchant_" + enchantment.getKey().getKey());

            int elytraLevel = container.getOrDefault(elytraEnchantKey, PersistentDataType.INTEGER, 0);
            int chestplateLevel = container.getOrDefault(chestplateEnchantKey, PersistentDataType.INTEGER, 0);

            // Keep only the highest level of the enchantment
            int highestLevel = Math.max(elytraLevel, chestplateLevel);

            if (highestLevel > 0) {
                displayEnchants.put(enchantment, highestLevel);
            }
        }

        // Build the lore lines from the clean map
        if (!displayEnchants.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : displayEnchants.entrySet()) {
                lines.add(String.format("§3» §b%s %d", getCapitalizedName(entry.getKey().getKey().getKey()), entry.getValue()));
            }
        }

        return lines;
    }

    private String getCapitalizedName(String name) {
        String[] words = name.toLowerCase().replace("_", " ").split(" ");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            capitalized.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return capitalized.toString().trim();
    }
}
