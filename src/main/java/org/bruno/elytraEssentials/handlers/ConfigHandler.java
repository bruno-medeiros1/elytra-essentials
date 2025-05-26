package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;

public class ConfigHandler {
    private final FileConfiguration fileConfiguration;

    // General section
    private boolean isElytraEquipDisabled;
    private boolean isDebugModeEnabled;

    // Flight section
    private boolean isGlobalFlightDisabled;
    private List<String> disabledWorlds;
    private boolean isSpeedLimitEnabled;
    private double defaultSpeedLimit;
    private HashMap<String, Double> perWorldSpeedLimits;

    public ConfigHandler(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
        SetConfigVariables();
    }

    public final void SetConfigVariables() {
        this.isElytraEquipDisabled = this.fileConfiguration.getBoolean("general.disable-elytra-equip", true);
        this.isDebugModeEnabled = this.fileConfiguration.getBoolean("general.debug-mode", false);

        this.isGlobalFlightDisabled = this.fileConfiguration.getBoolean("flight.disable-global", false);
        this.disabledWorlds = this.fileConfiguration.getStringList("flight.disabled-worlds");
        this.isSpeedLimitEnabled = this.fileConfiguration.getBoolean("flight.speed-limit.enabled", true);
        this.defaultSpeedLimit = this.fileConfiguration.getDouble("flight.speed-limit.default", 75);

        ConfigurationSection perWorldSection = this.fileConfiguration.getConfigurationSection("flight.speed-limit.per-world");
        this.perWorldSpeedLimits = new HashMap<>();

        if (perWorldSection != null) {
            for (String worldName : perWorldSection.getKeys(false)) {
                try {
                    // Try to parse the value as a double
                    double worldSpeedLimit = perWorldSection.getDouble(worldName, this.defaultSpeedLimit);
                    Bukkit.getLogger().info("World Speed Limit for " + worldName + " : " + worldSpeedLimit);
                    this.perWorldSpeedLimits.put(worldName, worldSpeedLimit);
                } catch (Exception e) {
                    MessagesHelper.SendDebugMessage("Invalid speed limit for world '" + worldName + "' in config.yml. Using default speed limit.");
                    this.perWorldSpeedLimits.put(worldName, this.defaultSpeedLimit);
                }
            }
        } else {
            MessagesHelper.SendDebugMessage("No per-world speed limits defined in config.yml. Using default values.");
        }
    }

    public final boolean getIsElytraEquipDisabled() { return this.isElytraEquipDisabled; }
    public final boolean getIsDebugModeEnabled() {
        return this.isDebugModeEnabled;
    }

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
}
