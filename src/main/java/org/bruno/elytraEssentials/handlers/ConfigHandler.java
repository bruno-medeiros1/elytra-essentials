package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;

public class ConfigHandler {
    private final FileConfiguration fileConfiguration;

    private boolean developerModeIsEnabled = true;
    private boolean disableAllElytraFlight = false;
    private boolean elytraSpeedLimitIsEnabled = true;

    private List disabledWorlds;
    private HashMap<String, Double> perWorldSpeedLimits;

    private double defaultMaxSpeed = 75;

    public ConfigHandler(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
        SetConfigVariables();
    }

    public final void SetConfigVariables() {
        this.developerModeIsEnabled = this.fileConfiguration.getBoolean("general.developer-debug-mode.enabled", true);
        this.disableAllElytraFlight = this.fileConfiguration.getBoolean("elytra-flight-event.disable-all-elytra-flight", false);
        this.elytraSpeedLimitIsEnabled = this.fileConfiguration.getBoolean("elytra-flight-event.speed-limit.enabled", true);
        this.defaultMaxSpeed = this.fileConfiguration.getDouble("elytra-flight-event.speed-limit.default-max-speed", 75);
        this.disabledWorlds = this.fileConfiguration.getStringList("elytra-flight-event.disabled-worlds");

        ConfigurationSection perWorldSection = this.fileConfiguration.getConfigurationSection("elytra-flight-event.speed-limit.per-world");
        this.perWorldSpeedLimits = new HashMap<>();

        if (perWorldSection != null) {
            for (String worldName : perWorldSection.getKeys(false)) {
                try {
                    // Try to parse the value as a double
                    double worldSpeedLimit = perWorldSection.getDouble(worldName, this.defaultMaxSpeed);
                    Bukkit.getLogger().info("World Speed Limit for " + worldName + " : " + worldSpeedLimit);
                    this.perWorldSpeedLimits.put(worldName, worldSpeedLimit);
                } catch (Exception e) {
                    MessagesHelper.SendDebugMessage("Invalid speed limit for world '" + worldName + "' in config.yml. Using default speed limit.");
                    this.perWorldSpeedLimits.put(worldName, this.defaultMaxSpeed);
                }
            }
        } else {
            MessagesHelper.SendDebugMessage("No per-world speed limits defined in config.yml. Using default values.");
        }
    }

    public final boolean getDeveloperModeIsEnabled() {
        return this.developerModeIsEnabled;
    }

    public final boolean getDisableAllElytraFlight() {
        return this.disableAllElytraFlight;
    }

    public final boolean getElytraSpeedLimitIsEnabled() {
        return this.elytraSpeedLimitIsEnabled;
    }

    public final List getDisabledWorlds() {
        return this.disabledWorlds;
    }

    public final double getDefaultMaxSpeed() {
        return this.defaultMaxSpeed;
    }

    public final HashMap<String, Double> getPerWorldSpeedLimits(){
        return this.perWorldSpeedLimits;
    }

}
