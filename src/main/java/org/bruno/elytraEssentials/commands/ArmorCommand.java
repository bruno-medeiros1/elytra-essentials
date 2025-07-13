package org.bruno.elytraEssentials.commands;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
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

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            handleInfoCommand(sender);
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("repair")) {
            handleRepairCommand(sender);
            return true;
        }

        plugin.getMessagesHelper().sendCommandSenderMessage(sender, "&cUsage: /ee armor <repair>");
        return true;
    }

    private void handleInfoCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesHelper().sendCommandSenderMessage(sender, "&cThis command can only be run by a player.");
            return;
        }

        if (!PermissionsHelper.hasArmorPermission(player)) {
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (!isArmoredElytra(chestplate)) {
            plugin.getMessagesHelper().sendPlayerMessage(player, "&cYou are not currently wearing an Armored Elytra.");
            return;
        }

        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null) {
            plugin.getMessagesHelper().sendPlayerMessage(player, "&cError: Could not read item data.");
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        int currentDurability = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG), PersistentDataType.INTEGER, 0);
        int maxDurability = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.MAX_ARMOR_DURABILITY_TAG), PersistentDataType.INTEGER, 1);
        String materialName = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.ARMOR_MATERIAL_TAG), PersistentDataType.STRING, "Unknown");
        materialName = materialName.replace("_CHESTPLATE", "").replace("_", " ");

        double damageAbsorbed = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.DAMAGE_ABSORBED_TAG), PersistentDataType.DOUBLE, 0.0);
        int timesShattered = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.PLATING_SHATTERED_TAG), PersistentDataType.INTEGER, 0);
        String forgedBy = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.FORGED_BY_TAG), PersistentDataType.STRING, "§kUnknown");

        //  Build and send the interactive message
        sendArmorInfoMessage(player, currentDurability, maxDurability, materialName, damageAbsorbed, timesShattered, forgedBy, container);
    }

    private void handleRepairCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesHelper().sendCommandSenderMessage(sender, "&cThis command can only be run by a player.");
            return;
        }

        if (!PermissionsHelper.hasRepairPermission(player)) {
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (!isArmoredElytra(chestplate)) {
            plugin.getMessagesHelper().sendPlayerMessage(player,"&eYou must be wearing an Armored Elytra to repair it.");
            return;
        }

        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey durabilityKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG);
        NamespacedKey maxDurabilityKey = new NamespacedKey(plugin, Constants.NBT.MAX_ARMOR_DURABILITY_TAG);

        int currentDurability = container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);
        int maxDurability = container.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, 1);

        if (currentDurability >= maxDurability) {
            plugin.getMessagesHelper().sendPlayerMessage(player,"&aYour Armored Elytra's plating is already at full durability!");
            return;
        }

        // Handle payment for the repair
        if (!handleRepairPayment(player)) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
            return;
        }

        // Set durability to max
        container.set(durabilityKey, PersistentDataType.INTEGER, maxDurability);

        // Update the lore
        updateDurabilityLore(meta);
        chestplate.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 1.2f);
        plugin.getMessagesHelper().sendPlayerMessage(player,"&aYour Armored Elytra's plating has been fully repaired!");
    }

    private boolean handleRepairPayment(Player player) {
        double moneyCost = plugin.getConfigHandlerInstance().getRepairCostMoney();
        int xpCost = plugin.getConfigHandlerInstance().getRepairCostXpLevels();

        // Check requirements first
        if (moneyCost > 0) {
            Economy economy = plugin.getEconomy();
            if (economy == null) return true;
            if (!economy.has(player, moneyCost)) {
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNotEnoughMoney());
                return false;
            }
        }
        if (xpCost > 0) {
            if (player.getLevel() < xpCost) {
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNotEnoughXP());
                return false;
            }
        }

        // Deduct costs
        if (moneyCost > 0) plugin.getEconomy().withdrawPlayer(player, moneyCost);
        if (xpCost > 0) player.setLevel(player.getLevel() - xpCost);

        return true;
    }

    private void updateDurabilityLore(ItemMeta meta) {
        NamespacedKey maxDurabilityKey = new NamespacedKey(plugin, Constants.NBT.MAX_ARMOR_DURABILITY_TAG);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int max = container.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, 1);

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        String durabilityLine = String.format("§6Armor Plating: §a%d / %d", max, max);

        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains("Armor Plating:")) {
                lore.set(i, durabilityLine);
                found = true;
                break;
            }
        }
        if (!found) lore.add(durabilityLine);

        meta.setLore(lore);
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (sender instanceof Player player) {
                if (!PermissionsHelper.hasRepairPermission(player))
                    return List.of();
            }

            return List.of("repair");
        }

        return List.of();
    }

    private void sendArmorInfoMessage(Player player, int currentDurability, int maxDurability, String materialName, double damageAbsorbed, int timesShattered, String forgedBy, PersistentDataContainer container) {
        String primaryColor = "§b";
        String secondaryColor = "§3";
        String textColor = "§7";
        String valueColor = "§f";
        String arrow = "» ";

        player.sendMessage(primaryColor + "§m----------------------------------------------------");
        player.sendMessage("");
        player.sendMessage(primaryColor + "§lArmored Elytra Info");
        player.sendMessage("");

        // Build the "Armor Plating" line with an interactive button if needed
        TextComponent platingTitle = new TextComponent(TextComponent.fromLegacyText(secondaryColor + arrow + textColor + "Armor Plating "));
        if (currentDurability < maxDurability) {
            double repairMoneyCost = plugin.getConfigHandlerInstance().getRepairCostMoney();
            int repairXpCost = plugin.getConfigHandlerInstance().getRepairCostXpLevels();

            TextComponent repairButton = new TextComponent("§b[Repair]");
            repairButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ee armor repair"));

            BaseComponent[] hoverText = new TextComponent[]{
                    new TextComponent(TextComponent.fromLegacyText(
                            "§eClick to fully repair the Armor Plating.\n" +
                                    "§eCost: §6$" + String.format("%.2f", repairMoneyCost) + " §eand §6" + repairXpCost + " §eLevels"
                    ))
            };
            repairButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));
            platingTitle.addExtra(repairButton);
        }
        player.spigot().sendMessage(platingTitle);

        // Display Armor Plating Durability with the visual bar
        player.sendMessage(createDurabilityBar(currentDurability, maxDurability));

        player.sendMessage("");
        player.sendMessage(secondaryColor + arrow + textColor + "Base Material: " + valueColor + getCapitalizedName(materialName));

        List<String> enchantmentLines = getEnchantmentLines(container);
        if (!enchantmentLines.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(secondaryColor + "Stored Enchantments:");
            for (String line : enchantmentLines) {
                player.sendMessage(line);
            }
        }

        player.sendMessage("");
        player.sendMessage(secondaryColor + "§lItem History:");
        double damageInHearts = damageAbsorbed / 2.0;
        player.sendMessage(String.format(" %s- %sTotal Damage Absorbed: %s%.1f ❤", "§f", textColor, "§a", damageInHearts));
        player.sendMessage(String.format(" %s- %sTimes Plating Shattered: %s%d", "§f", textColor, "§c", timesShattered));
        player.sendMessage(String.format(" %s- %sForged By: %s%s", "§f", textColor, "§b", forgedBy));

        player.sendMessage("");
        player.sendMessage(primaryColor + "§m----------------------------------------------------");
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

        int totalSegments = 28; // The total width of the bar in characters
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
