package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.UpgradeType;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class UpgradeHandler {
    private final ElytraEssentials plugin;

    public UpgradeHandler(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculates the bonus speed from the Max Velocity upgrade on an ItemStack.
     * @param elytra The Armored Elytra to check.
     * @return The bonus speed in km/h, or 0.0 if no upgrade is present.
     */
    public double getBonusSpeed(ItemStack elytra) {
        // TODO (b.med): Should player progression/upgrades be allowed to override admin-set world rules (speed limits)?
        int level = getUpgradeLevel(elytra, UpgradeType.MAX_VELOCITY);
        return level > 0 ? level * UpgradeType.MAX_VELOCITY.getValuePerLevel() : 0.0;
    }

    /**
     * Calculates the chance to save flight time from the Flight Efficiency upgrade.
     * @param elytra The Armored Elytra to check.
     * @return The percentage chance (e.g., 25.0 for 25%) to not consume flight time.
     */
    public double getFlightEfficiencyChance(ItemStack elytra) {
        int level = getUpgradeLevel(elytra, UpgradeType.FLIGHT_EFFICIENCY);
        return level > 0 ? level * UpgradeType.FLIGHT_EFFICIENCY.getValuePerLevel() : 0.0;
    }

    /**
     * Calculates the percentage increase in power for the boost item.
     * @param elytra The Armored Elytra the player is wearing.
     * @return The percentage bonus (e.g., 20.0 for 20%).
     */
    public double getBonusBoostPower(ItemStack elytra) {
        int level = getUpgradeLevel(elytra, UpgradeType.BOOST_POWER);
        return level > 0 ? level * UpgradeType.BOOST_POWER.getValuePerLevel() : 0.0;
    }

    /**
     * Calculates the bonus to maximum durability for the armor plating.
     * @param elytra The Armored Elytra to check.
     * @return The flat bonus to add to the maximum durability.
     */
    public int getBonusArmorDurability(ItemStack elytra) {
        int level = getUpgradeLevel(elytra, UpgradeType.ARMOR_DURABILITY);
        return level > 0 ? (int) (level * UpgradeType.ARMOR_DURABILITY.getValuePerLevel()) : 0;
    }

    /**
     * Calculates the chance to negate damage from flying into a wall.
     * @param elytra The Armored Elytra to check.
     * @return The percentage chance (e.g., 15.0 for 15%) to negate damage.
     */
    public double getKineticResistanceChance(ItemStack elytra) {
        int level = getUpgradeLevel(elytra, UpgradeType.KINETIC_RESISTANCE);
        return level > 0 ? level * UpgradeType.KINETIC_RESISTANCE.getValuePerLevel() : 0.0;
    }

    /**
     * Calculates the bonus armor points from the Armor Protection upgrade.
     * @param elytra The Armored Elytra to check.
     * @return The flat bonus to add to the player's armor attribute.
     */
    public double getBonusArmor(ItemStack elytra) {
        int level = getUpgradeLevel(elytra, UpgradeType.ARMOR_PROTECTION);
        return level > 0 ? level * UpgradeType.ARMOR_PROTECTION.getValuePerLevel() : 0.0;
    }

    /**
     * Calculates the bonus armor toughness from the Armor Toughness upgrade.
     * @param elytra The Armored Elytra to check.
     * @return The flat bonus to add to the player's armor toughness attribute.
     */
    public double getBonusToughness(ItemStack elytra) {
        int level = getUpgradeLevel(elytra, UpgradeType.ARMOR_TOUGHNESS);
        return level > 0 ? level * UpgradeType.ARMOR_TOUGHNESS.getValuePerLevel() : 0.0;
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
}