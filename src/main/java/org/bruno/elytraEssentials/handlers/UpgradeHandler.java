package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.ElytraUpgrade;
import org.bruno.elytraEssentials.utils.UpgradeType;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class UpgradeHandler {
    private final ElytraEssentials plugin;
    private final Logger logger;

    private FileConfiguration fileConfiguration;

    private final HashMap<UpgradeType, ElytraUpgrade> upgradeValues = new HashMap<>();

    public UpgradeHandler(ElytraEssentials plugin, FileConfiguration fileConfiguration, Logger logger) {
        this.plugin = plugin;
        this.fileConfiguration = fileConfiguration;
        this.logger = logger;

        loadUpgrades();
    }

    public void reload(FileConfiguration newFileConfiguration) {
        this.fileConfiguration = newFileConfiguration;
        loadUpgrades();
    }

    public void loadUpgrades() {
        ConfigurationSection section = fileConfiguration.getConfigurationSection("upgrades");
        if (section == null) {
            logger.warning("No section 'upgrades' found in upgrades.yml.");
            return;
        }

        for (UpgradeType type : UpgradeType.values()) {
            ConfigurationSection upgrade = section.getConfigurationSection(type.name().toLowerCase());
            if (upgrade != null) {
                String description = upgrade.getString("description", type.getDescription());
                int maxLevel = upgrade.getInt("max-level", type.getMaxLevel());
                double bonus = upgrade.getDouble("bonus-per-level", type.getValuePerLevel());
                double baseCost = upgrade.getDouble("base-cost", 1000);
                double costMultiplier = upgrade.getDouble("cost-multiplier", 1.8);
                upgradeValues.put(type, new ElytraUpgrade(maxLevel, bonus, description, baseCost, costMultiplier));
            }
        }
    }

    public Map<UpgradeType, ElytraUpgrade> getUpgradeValues() {
        return upgradeValues;
    }

    /**
     * Calculates the bonus speed from the Max Velocity upgrade on an ItemStack.
     * @param elytra The Armored Elytra to check.
     * @return The bonus speed in km/h, or 0.0 if no upgrade is present.
     */
    public double getBonusSpeed(ItemStack elytra) {
        return getUpgradeBonus(elytra, UpgradeType.MAX_VELOCITY);  // TODO (b.med): Should player progression/upgrades be allowed to override admin-set world rules (speed limits)?
    }

    /**
     * Calculates the chance to save flight time from the Flight Efficiency upgrade.
     * @param elytra The Armored Elytra to check.
     * @return The percentage chance (e.g., 25.0 for 25%) to not consume flight time.
     */
    public double getFlightEfficiencyChance(ItemStack elytra) {
        return getUpgradeBonus(elytra, UpgradeType.FLIGHT_EFFICIENCY);
    }

    /**
     * Calculates the percentage increase in power for the boost item.
     * @param elytra The Armored Elytra the player is wearing.
     * @return The percentage bonus (e.g., 20.0 for 20%).
     */
    public double getBonusBoostPower(ItemStack elytra) {
        return getUpgradeBonus(elytra, UpgradeType.BOOST_POWER);
    }

    /**
     * Calculates the bonus to maximum durability for the armor plating.
     * @param elytra The Armored Elytra to check.
     * @return The flat bonus to add to the maximum durability.
     */
    public int getBonusArmorDurability(ItemStack elytra) {
        return (int) getUpgradeBonus(elytra, UpgradeType.ARMOR_DURABILITY);
    }

    /**
     * Calculates the chance to negate damage from flying into a wall.
     * @param elytra The Armored Elytra to check.
     * @return The percentage chance (e.g., 15.0 for 15%) to negate damage.
     */
    public double getKineticResistanceChance(ItemStack elytra) {
        return getUpgradeBonus(elytra, UpgradeType.KINETIC_RESISTANCE);
    }

    /**
     * Calculates the bonus armor points from the Armor Protection upgrade.
     * @param elytra The Armored Elytra to check.
     * @return The flat bonus to add to the player's armor attribute.
     */
    public double getBonusArmor(ItemStack elytra) {
        return getUpgradeBonus(elytra, UpgradeType.ARMOR_PROTECTION);
    }

    /**
     * Calculates the bonus armor toughness from the Armor Toughness upgrade.
     * @param elytra The Armored Elytra to check.
     * @return The flat bonus to add to the player's armor toughness attribute.
     */
    public double getBonusToughness(ItemStack elytra) {
        return getUpgradeBonus(elytra, UpgradeType.ARMOR_TOUGHNESS);
    }

    /**
     * A generic private helper to safely get an upgrade level from an item's NBT.
     * @param elytra The ItemStack to check.
     * @param type The UpgradeType to get the level for.
     * @return The integer level of the upgrade, or 0 if not present.
     */
    private int getUpgradeLevel(ItemStack elytra, UpgradeType type) {
        if (elytra == null || !elytra.hasItemMeta()) return 0;
        ItemMeta meta = elytra.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, type.getKey());
        return container.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    /**
     * Generic helper to calculate the bonus for any upgrade.
     * Uses the upgrade level from the ItemStack and the bonus-per-level from config.
     * @param elytra The ItemStack to check.
     * @param type The UpgradeType to calculate the bonus for.
     * @return The total bonus for this upgrade.
     */
    private double getUpgradeBonus(ItemStack elytra, UpgradeType type) {
        int level = getUpgradeLevel(elytra, type);
        if (level <= 0) return 0.0;

        ElytraUpgrade data = upgradeValues.get(type);
        if (data == null)
            data = new ElytraUpgrade(type.getMaxLevel(), type.getValuePerLevel(), type.getDescription(), 1000, 1.8);

        // Clamp the level to the maxLevel just in case
        level = Math.min(level, data.getMaxLevel());

        return level * data.getBonusPerLevel();
    }
}