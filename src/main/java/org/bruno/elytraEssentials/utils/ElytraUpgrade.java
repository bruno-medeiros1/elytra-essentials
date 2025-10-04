package org.bruno.elytraEssentials.utils;

public class ElytraUpgrade {
    private final int maxLevel;
    private final double bonusPerLevel;
    private final String description;
    private final double baseCost;
    private final double costMultiplier;

    public ElytraUpgrade(int maxLevel, double bonusPerLevel, String description, double baseCost, double costMultiplier) {
        this.maxLevel = maxLevel;
        this.bonusPerLevel = bonusPerLevel;
        this.description = description;
        this.baseCost = baseCost;
        this.costMultiplier = costMultiplier;
    }

    public int getMaxLevel() { return maxLevel; }
    public double getBonusPerLevel() { return bonusPerLevel; }
    public String getDescription() { return description; }
    public double getBaseCost() { return baseCost; }
    public double getCostMultiplier() { return costMultiplier; }
}