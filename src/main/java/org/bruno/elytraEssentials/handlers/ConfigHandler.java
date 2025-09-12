package org.bruno.elytraEssentials.handlers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class ConfigHandler {
    private final Logger logger;

    private FileConfiguration fileConfiguration;

    // General section
    private boolean isDebugModeEnabled;
    private boolean isCheckForUpdatesEnabled;
    private boolean isElytraEquipDisabled;
    private boolean isElytraBreakProtectionEnabled;
    private boolean isKineticEnergyProtectionEnabled;
    private boolean isEmergencyDeployEnabled;
    private boolean isFireworkBoostingDisabled;
    private boolean isRiptideLaunchDisabled;
    private boolean isLiquidGlideEnabled;

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

    //  Database section
    private String storageType;
    private boolean isAutoBackupEnabled;
    private int autoBackupInterval;
    private int autoBackupMaxBackups;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    //  Boost section
    private boolean isBoostEnabled;
    private String boostItem;
    private int boostCooldown;
    private boolean isChargedJumpEnabled;
    private int chargeTime;
    private double jumpStrength;

    //  Armored Elytra section
    private boolean isArmoredElytraEnabled;
    private double forgeCostMoney;
    private int forgeCostXpLevels;
    private double repairCostMoney;
    private int repairCostXpLevels;

    //  Combat Tag
    private boolean isCombatTagEnabled;
    private int combatTagCooldown;
    private boolean isCombatTagPreventFallDamageEnabled;
    private boolean isCombatTagPlayerDamageOnlyEnabled;

    //  Tandem Flight
    private boolean isTandemFlightEnabled;
    private int tandemInviteTimeout;
    private double tandemFlightTimeCostMultiplier;
    private boolean tandemFallDamageProtection;
    private int tandemMountCountdown;
    private boolean isLaunchAnimationEnabled;

    public ConfigHandler(FileConfiguration fileConfiguration, Logger logger) {
        this.fileConfiguration = fileConfiguration;
        this.logger = logger;

        setConfigVariables();
    }

    public final void setConfigVariables() {
        this.isDebugModeEnabled = this.fileConfiguration.getBoolean("general.debug-mode", false);
        this.isCheckForUpdatesEnabled = this.fileConfiguration.getBoolean("general.check-for-updates", true);
        this.isElytraEquipDisabled = this.fileConfiguration.getBoolean("general.disable-elytra-equipment", false);
        this.isElytraBreakProtectionEnabled = this.fileConfiguration.getBoolean("general.elytra-break-protection", false);
        this.isKineticEnergyProtectionEnabled = this.fileConfiguration.getBoolean("general.kinetic-energy-protection", false);
        this.isEmergencyDeployEnabled = this.fileConfiguration.getBoolean("general.emergency-deploy", false);
        this.isFireworkBoostingDisabled = this.fileConfiguration.getBoolean("general.disable-firework-boosting", false);
        this.isRiptideLaunchDisabled = this.fileConfiguration.getBoolean("general.disable-riptide-launch", false);
        this.isLiquidGlideEnabled = this.fileConfiguration.getBoolean("general.allow-liquid-glide", true);

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
                    logger.info("Invalid speed limit for world '" + worldName + "' in config.yml. Using default speed limit.");
                    this.perWorldSpeedLimits.put(worldName, this.defaultSpeedLimit);
                }
            }
        } else {
            logger.info("No per-world speed limits defined in config.yml. Using default values.");
        }

        this.isTimeLimitEnabled = this.fileConfiguration.getBoolean("flight.time-limit.enabled", false);
        this.maxTimeLimit = this.fileConfiguration.getInt("flight.time-limit.max-time", 600);

        this.isRecoveryEnabled = this.fileConfiguration.getBoolean("flight.time-limit.recovery.enabled", true);
        this.recoveryAmount = this.fileConfiguration.getInt("flight.time-limit.recovery.amount", 10);
        this.recoveryInterval = this.fileConfiguration.getInt("flight.time-limit.recovery.interval", 60);

        this.storageType = this.fileConfiguration.getString("storage.type", "SQLITE");
        this.isAutoBackupEnabled = this.fileConfiguration.getBoolean("storage.auto-backup.enabled", true);
        this.autoBackupInterval = this.fileConfiguration.getInt("storage.auto-backup.interval", 60);
        this.autoBackupMaxBackups = this.fileConfiguration.getInt("storage.auto-backup.max-backups", 24);

        this.host = this.fileConfiguration.getString("storage.mysql.host", "localhost");
        this.port = this.fileConfiguration.getInt("storage.mysql.port", 3306);
        this.database = this.fileConfiguration.getString("storage.mysql.database", "elytraessentials");
        this.username = this.fileConfiguration.getString("storage.mysql.user", "root");
        this.password = this.fileConfiguration.getString("storage.mysql.password", "");

        this.isBoostEnabled = this.fileConfiguration.getBoolean("flight.boost.enabled", true);
        this.boostItem = this.fileConfiguration.getString("flight.boost.item", "FEATHER");
        this.boostCooldown = this.fileConfiguration.getInt("flight.boost.cooldown", 1000);
        this.isChargedJumpEnabled = this.fileConfiguration.getBoolean("flight.boost.charged-jump.enabled", true);
        this.chargeTime = this.fileConfiguration.getInt("flight.boost.charged-jump.charge-time", 2);
        this.jumpStrength = this.fileConfiguration.getDouble("flight.boost.charged-jump.jump-strength", 1.5);

        this.isArmoredElytraEnabled = this.fileConfiguration.getBoolean("armored-elytra.enabled", true);
        this.forgeCostMoney = this.fileConfiguration.getDouble("armored-elytra.forging-cost.money", 5000);
        this.forgeCostXpLevels = this.fileConfiguration.getInt("armored-elytra.forging-cost.xp-levels", 10);
        this.repairCostMoney = this.fileConfiguration.getDouble("armored-elytra.repair-cost.money", 500);
        this.repairCostXpLevels = this.fileConfiguration.getInt("armored-elytra.repair-cost.xp-levels", 5);

        this.isCombatTagEnabled = this.fileConfiguration.getBoolean("flight.combat-tag.enabled", true);
        this.combatTagCooldown = this.fileConfiguration.getInt("flight.combat-tag.duration", 10);
        this.isCombatTagPreventFallDamageEnabled = this.fileConfiguration.getBoolean("flight.combat-tag.prevent-fall-damage", true);
        this.isCombatTagPlayerDamageOnlyEnabled = this.fileConfiguration.getBoolean("flight.combat-tag.player-damage-only", true);

        this.isTandemFlightEnabled = this.fileConfiguration.getBoolean("flight.tandem.enabled", true);
        this.tandemInviteTimeout = this.fileConfiguration.getInt("flight.tandem.invite-timeout", 30);
        this.tandemFlightTimeCostMultiplier = this.fileConfiguration.getDouble("flight.tandem.flight-time-cost-multiplier", 2.0);
        this.tandemFallDamageProtection = this.fileConfiguration.getBoolean("flight.tandem.fall-damage-protection", true);
        this.tandemMountCountdown = this.fileConfiguration.getInt("flight.tandem.mount-countdown", 3);
        this.isLaunchAnimationEnabled = this.fileConfiguration.getBoolean("flight.tandem.enable-launch-animation", true);
    }

    /**
     * Reloads the configuration values from a new FileConfiguration object.
     * This is called from the main plugin's reload sequence.
     * @param newFileConfiguration The newly reloaded config object.
     */
    public void reload(FileConfiguration newFileConfiguration) {
        this.fileConfiguration = newFileConfiguration;
        setConfigVariables();
        logger.info("Configuration values have been reloaded.");
    }

    public final boolean getIsDebugModeEnabled() {
        return this.isDebugModeEnabled;
    }
    public final boolean getIsCheckForUpdatesEnabled() { return this.isCheckForUpdatesEnabled; }
    public final boolean getIsElytraEquipDisabled() { return this.isElytraEquipDisabled; }
    public final boolean getIsElytraBreakProtectionEnabled() { return this.isElytraBreakProtectionEnabled; }
    public final boolean getIsKineticEnergyProtectionEnabled() { return this.isKineticEnergyProtectionEnabled; }
    public final boolean getIsEmergencyDeployEnabled() { return this.isEmergencyDeployEnabled; }
    public final boolean getIsFireworkBoostingDisabled() { return this.isFireworkBoostingDisabled; }
    public final boolean getIsRiptideLaunchDisabled() { return this.isRiptideLaunchDisabled; }
    public final boolean getIsLiquidGlideEnabled() { return this.isLiquidGlideEnabled; }

    public final boolean getIsGlobalFlightDisabled() {
        return this.isGlobalFlightDisabled;
    }
    public final List<String> getDisabledWorlds() {
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

    public final String getStorageType() { return this.storageType; }
    public final boolean getIsAutoBackupEnabled() { return this.isAutoBackupEnabled; }
    public final int getAutoBackupInterval() { return this.autoBackupInterval; }
    public final int getAutoBackupMaxBackups() { return this.autoBackupMaxBackups; }
    public final String getHost() { return this.host; }
    public final int getPort() { return this.port; }
    public final String getDatabase() { return this.database; }
    public final String getUsername() { return this.username; }
    public final String getPassword() { return this.password; }

    public final boolean getIsBoostEnabled() { return this.isBoostEnabled; }
    public final String getBoostItem() { return this.boostItem; }
    public final Integer getBoostCooldown() { return this.boostCooldown; }
    public final boolean getIsChargedJumpEnabled() { return this.isChargedJumpEnabled; }
    public final int getChargeTime() { return this.chargeTime; }
    public final double getJumpStrength() { return this.jumpStrength; }

    public final boolean getIsArmoredElytraEnabled() { return this.isArmoredElytraEnabled; }
    public final double getForgeCostMoney() { return this.forgeCostMoney; }
    public final int getForgeCostXpLevels() { return this.forgeCostXpLevels; }
    public final double getRepairCostMoney() { return this.repairCostMoney; }
    public final int getRepairCostXpLevels() { return this.repairCostXpLevels; }

    public final boolean getIsCombatTagEnabled() { return this.isCombatTagEnabled; }
    public final int getCombatTagCooldown() { return this.combatTagCooldown; }
    public final boolean getIsCombatTagPreventFallDamageEnabled() { return this.isCombatTagPreventFallDamageEnabled; }
    public final boolean getIsCombatTagPlayerDamageOnlyEnabled() { return this.isCombatTagPlayerDamageOnlyEnabled; }

    public final boolean getIsTandemFlightEnabled() { return this.isTandemFlightEnabled; }
    public final int getTandemInviteTimeout() { return this.tandemInviteTimeout; }
    public final double getTandemFlightTimeCostMultiplier() { return this.tandemFlightTimeCostMultiplier; }
    public final boolean getTandemFallDamageProtection() { return this.tandemFallDamageProtection; }
    public final int getTandemMountCountdown() { return this.tandemMountCountdown; }
    public final boolean getIsLaunchAnimationEnabled() { return this.isLaunchAnimationEnabled; }
}
