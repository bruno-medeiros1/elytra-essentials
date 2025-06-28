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
import java.util.List;

public class ArmorCommand implements ISubCommand {
    private final ElytraEssentials plugin;

    public ArmorCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

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

        // --- Formatting inspired by your /ee help command ---
        ChatColor primary = ChatColor.AQUA;
        ChatColor secondary = ChatColor.DARK_AQUA;
        ChatColor text = ChatColor.GRAY;
        ChatColor value = ChatColor.WHITE;
        String arrow = "» ";

        // --- Build and send the message ---
        player.sendMessage(primary + "§m----------------------------------------------------");
        player.sendMessage("");
        player.sendMessage(primary + "§lArmored Elytra Info");
        player.sendMessage("");

        // Display Armor Plating Durability
        double percentage = (maxDurability > 0) ? (double) currentDurability / maxDurability : 0;
        String durabilityColor;
        if (percentage > 0.6) durabilityColor = "§a"; // Green
        else if (percentage > 0.2) durabilityColor = "§e"; // Yellow
        else durabilityColor = "§c"; // Red
        player.sendMessage(secondary + arrow + text + "Armor Plating: " + durabilityColor + currentDurability + " / " + maxDurability);

        // Display Base Material
        player.sendMessage(secondary + arrow + text + "Base Material: " + value + getCapitalizedName(materialName));

        // Display Stored Enchantments
        List<String> enchantmentLines = getEnchantmentLines(container);
        if (!enchantmentLines.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(secondary + "Stored Enchantments:");
            for (String line : enchantmentLines) {
                player.sendMessage(line);
            }
        }

        player.sendMessage("");
        player.sendMessage(primary + "§m----------------------------------------------------");

        return true;
    }

    private boolean isArmoredElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(new NamespacedKey(plugin, Constants.NBT.ARMORED_ELYTRA_TAG), PersistentDataType.BOOLEAN);
    }

    private List<String> getEnchantmentLines(PersistentDataContainer container) {
        List<String> lines = new ArrayList<>();
        ChatColor valueColor = ChatColor.AQUA;
        ChatColor textColor = ChatColor.GRAY;

        for (Enchantment enchantment : Enchantment.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "enchant_" + enchantment.getKey().getKey());
            if (container.has(key, PersistentDataType.INTEGER)) {
                int level = container.get(key, PersistentDataType.INTEGER);
                lines.add(String.format(" %s- %s%s %d", textColor, valueColor, getCapitalizedName(enchantment.getKey().getKey()), level));
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
