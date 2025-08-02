package org.bruno.elytraEssentials.utils;

import org.bukkit.Material;

public enum UpgradeType {
    MAX_VELOCITY(Constants.NBT.MAX_VELOCITY_UPGRADE_TAG, "Max Velocity", "Increases the maximum speed limit\nyou can reach while gliding.",
            Material.EMERALD, 5, 10, " km/h"),
    FLIGHT_EFFICIENCY(Constants.NBT.FLIGHT_EFFICIENCY_UPGRADE_TAG, "Flight Efficiency", "Grants a chance to not consume\nflight time each second, making\nyour flights last longer.",
            Material.PHANTOM_MEMBRANE, 5, 10, "%"),
    BOOST_POWER(Constants.NBT.BOOST_POWER_UPGRADE_TAG, "Boost Power", "Increases the forward thrust gained\nfrom using the boost item.",
            Material.FEATHER, 5, 10, "%"),
    ARMOR_DURABILITY(Constants.NBT.ARMOR_DURABILITY_UPGRADE_TAG, "Armor Durability", "Increases the maximum durability of the\nelytra's armor plating, allowing it\nto absorb more damage.",
            Material.ANVIL, 3, 50, ""),
    KINETIC_RESISTANCE(Constants.NBT.KINETIC_RESISTANCE_UPGRADE_TAG, "Kinetic Resistance", "Grants a chance to completely negate\ndamage and knockback from flying into walls.",
            Material.BLAZE_POWDER, 3, 15, "%"),
    ARMOR_PROTECTION(Constants.NBT.ARMOR_PROTECTION_UPGRADE_TAG, "Armor Protection", "Adds a permanent bonus to the armor points\nprovided by your armored elytra.",
            Material.SHIELD, 2, 0.5, ""),
    ARMOR_TOUGHNESS(Constants.NBT.ARMOR_TOUGHNESS_UPGRADE_TAG, "Armor Toughness", "Adds a permanent bonus to the armor\ntoughness provided by your armored\nelytra, reducing high damage.",
            Material.NETHERITE_INGOT, 2, 0.5, "");

    private final String key;
    private final String displayName;
    private final String description;
    private final Material displayMaterial;
    private final int maxLevel;
    private final double valuePerLevel;
    private final String suffix;

    UpgradeType(String key, String displayName, String description, Material displayMaterial, int maxLevel, double valuePerLevel, String suffix) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
        this.displayMaterial = displayMaterial;
        this.maxLevel = maxLevel;
        this.valuePerLevel = valuePerLevel;
        this.suffix = suffix;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Material getDisplayMaterial() { return displayMaterial; }
    public int getMaxLevel() { return maxLevel; }
    public double getValuePerLevel() { return valuePerLevel; }
    public String getSuffix() { return suffix; }
}
