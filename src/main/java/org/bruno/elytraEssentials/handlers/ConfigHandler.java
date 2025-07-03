package org.bruno.elytraEssentials.handlers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;

public class ConfigHandler {
    private final FileConfiguration fileConfiguration;

    // General section
    private boolean isDebugModeEnabled;
    private boolean isCheckForUpdatesEnabled;
    private boolean isElytraEquipDisabled;
    private boolean isElytraBreakProtectionEnabled;
    private boolean isKineticEnergyProtectionEnabled;

    // Flight section
    private boolean isGlobalFlightDisabled;
    private List<String> disabledWorlds;
    private boolean isSpeedLimitEnabled;
    private double defaultSpeedLimit;
    private HashMap<String, Double> perWorldSpeedLimits;
    private boolean isTimeLimitEnabled;
    private int maxTimeLimit;

    //  recovery time
    private boolean isRecoveryEnabled;
    private int recoveryAmount;
    private int recoveryInterval;
    private boolean isNotifyOnRecoveryEnabled;

    //  Database section
    private String storageType;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    //  Boost section
    private boolean isBoostEnabled;
    private String boostItem;
    private int boostCooldown;

    //  Armored Elytra section
    private boolean isArmoredElytraEnabled;
    private double forgeCostMoney;
    private int forgeCostXpLevels;

    //  Combat Tag
    private boolean isCombatTagEnabled;
    private int combatTagCooldown;
    private boolean isCombatTagPreventFallDamageEnabled;
    private boolean isCombatTagPlayerDamageOnlyEnabled;

    public ConfigHandler(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
        SetConfigVariables();
    }

    public final void SetConfigVariables() {
        this.isDebugModeEnabled = this.fileConfiguration.getBoolean("general.debug-mode", false);
        this.isCheckForUpdatesEnabled = this.fileConfiguration.getBoolean("general.check-for-updates", true);
        this.isElytraEquipDisabled = this.fileConfiguration.getBoolean("general.disable-elytra-equipment", false);
        this.isElytraBreakProtectionEnabled = this.fileConfiguration.getBoolean("general.elytra-break-protection", false);
        this.isKineticEnergyProtectionEnabled = this.fileConfiguration.getBoolean("general.kinetic-energy-protection", false);

        this.isGlobalFlightDisabled = this.fileConfiguration.getBoolean("flight.disable-global", false);
        this.disabledWorlds = this.fileConfiguration.getStringList("flight.disabled-worlds");
        this.isSpeedLimitEnabled = this.fileConfiguration.getBoolean("flight.speed-limit.enabled", true);
        this.defaultSpeedLimit = this.fileConfiguration.getDouble("flight.speed-limit.default", 75);

        ConfigurationSection perWorldSpeedLimitSection = this.fileConfiguration.getConfigurationSection("flight.speed-limit.per-world");
        this.perWorldSpeedLimits = new HashMap<>();

        if (perWorldSpeedLimitSection != null) {
            for (String worldName : perWorldSpeedLimitSection.getKeys(false)) {
                try {
                    // Try to parse the value as a double
                    double worldSpeedLimit = perWorldSpeedLimitSection.getDouble(worldName, this.defaultSpeedLimit);
                    this.perWorldSpeedLimits.put(worldName, worldSpeedLimit);
                } catch (Exception e) {
                    Bukkit.getLogger().info("Invalid speed limit for world '" + worldName + "' in config.yml. Using default speed limit.");
                    this.perWorldSpeedLimits.put(worldName, this.defaultSpeedLimit);
                }
            }
        } else {
            Bukkit.getLogger().info("No per-world speed limits defined in config.yml. Using default values.");
        }

        this.isTimeLimitEnabled = this.fileConfiguration.getBoolean("flight.time-limit.enabled", false);
        this.maxTimeLimit = this.fileConfiguration.getInt("flight.time-limit.max-time", 600);

        this.isRecoveryEnabled = this.fileConfiguration.getBoolean("flight.time-limit.recovery.enabled", true);
        this.recoveryAmount = this.fileConfiguration.getInt("flight.time-limit.recovery.amount", 10);
        this.recoveryInterval = this.fileConfiguration.getInt("flight.time-limit.recovery.interval", 60);
        this.isNotifyOnRecoveryEnabled = this.fileConfiguration.getBoolean("flight.time-limit.recovery.notify", true);

        this.storageType = this.fileConfiguration.getString("storage.type", "SQLITE");
        this.host = this.fileConfiguration.getString("storage.mysql.host", "localhost");
        this.port = this.fileConfiguration.getInt("storage.mysql.port", 3306);
        this.database = this.fileConfiguration.getString("storage.mysql.database", "elytraessentials");
        this.username = this.fileConfiguration.getString("storage.mysql.user", "root");
        this.password = this.fileConfiguration.getString("storage.mysql.password", "");

        this.isBoostEnabled = this.fileConfiguration.getBoolean("flight.boost.enabled", true);
        this.boostItem = this.fileConfiguration.getString("flight.boost.item", "FEATHER");
        this.boostCooldown = this.fileConfiguration.getInt("flight.boost.cooldown", 2000);

        this.isArmoredElytraEnabled = this.fileConfiguration.getBoolean("armored-elytra.enabled", true);
        this.forgeCostMoney = this.fileConfiguration.getDouble("armored-elytra.cost.money", 5000);
        this.forgeCostXpLevels = this.fileConfiguration.getInt("armored-elytra.cost.xp-levels", 10);

        this.isCombatTagEnabled = this.fileConfiguration.getBoolean("flight.combat-tag.enabled", true);
        this.combatTagCooldown = this.fileConfiguration.getInt("flight.combat-tag.duration", 10);
        this.isCombatTagPreventFallDamageEnabled = this.fileConfiguration.getBoolean("flight.combat-tag.prevent-fall-damage", true);
        this.isCombatTagPlayerDamageOnlyEnabled = this.fileConfiguration.getBoolean("flight.combat-tag.player-damage-only", true);
    }

    public final boolean getIsDebugModeEnabled() {
        return this.isDebugModeEnabled;
    }
    public final boolean getIsCheckForUpdatesEnabled() { return this.isCheckForUpdatesEnabled; }
    public final boolean getIsElytraEquipDisabled() { return this.isElytraEquipDisabled; }
    public final boolean getIsElytraBreakProtectionEnabled() { return this.isElytraBreakProtectionEnabled; }
    public final boolean getIsKineticEnergyProtectionEnabled() { return this.isKineticEnergyProtectionEnabled; }

    public final boolean getIsGlobalFlightDisabled() {
        return this.isGlobalFlightDisabled;
    }
    public final List getDisabledWorlds() {
        return this.disabledWorlds;
    }
    public final boolean getIsSpeedLimitEnabled() { return this.isSpeedLimitEnabled; }
    public final double getDefaultSpeedLimit() {
        return this.defaultSpeedLimit;
    }
    public final HashMap<String, Double> getPerWorldSpeedLimits(){ return this.perWorldSpeedLimits; }

    public final boolean getIsTimeLimitEnabled() { return this.isTimeLimitEnabled; }
    public final int getMaxTimeLimit() { return this.maxTimeLimit; }

    public final boolean getIsRecoveryEnabled() { return this.isRecoveryEnabled; }
    public final int getRecoveryAmount() { return this.recoveryAmount; }
    public final int getRecoveryInterval() { return this.recoveryInterval; }
    public final boolean getIsNotifyOnRecoveryEnabled() { return this.isNotifyOnRecoveryEnabled; }

    public String getStorageType() { return this.storageType; }
    public String getHost() { return this.host; }
    public int getPort() { return this.port; }
    public String getDatabase() { return this.database; }
    public String getUsername() { return this.username; }
    public String getPassword() { return this.password; }

    public final Boolean getIsBoostEnabled() { return this.isBoostEnabled; }
    public final String getBoostItem() { return this.boostItem; }
    public final Integer getBoostCooldown() { return this.boostCooldown; }

    public final boolean getIsArmoredElytraEnabled() { return this.isArmoredElytraEnabled; }
    public final double getForgeCostMoney() { return this.forgeCostMoney; }
    public final int getForgeCostXpLevels() { return this.forgeCostXpLevels; }

    public final boolean getIsCombatTagEnabled() { return this.isCombatTagEnabled; }
    public final int getCombatTagCooldown() { return this.combatTagCooldown; }
    public final boolean getIsCombatTagPreventFallDamageEnabled() { return this.isCombatTagPreventFallDamageEnabled; }
    public final boolean getIsCombatTagPlayerDamageOnlyEnabled() { return this.isCombatTagPlayerDamageOnlyEnabled; }
}
