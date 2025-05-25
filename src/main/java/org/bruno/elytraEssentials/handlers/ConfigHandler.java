package org.bruno.elytraEssentials.handlers;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigHandler {
    private final FileConfiguration fileConfiguration;

    private boolean developerModeIsEnabled = false;
    private boolean disableAllElytraFlight = false;
    private boolean elytraSpeedLimitIsEnabled = true;

    private List disabledWorlds;

    private double elytraMaxSpeed = 75;

    public ConfigHandler(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
    }

    public final void SetConfigVariables() {
        this.developerModeIsEnabled = this.fileConfiguration.getBoolean("general.developer-debug-mode.enabled", false);
        this.disableAllElytraFlight = this.fileConfiguration.getBoolean("elytra-flight-event.disable-all-elytra-flight", false);
        this.elytraSpeedLimitIsEnabled = this.fileConfiguration.getBoolean("elytra-flight-event.speed-limit.enabled", true);
        this.elytraMaxSpeed = this.fileConfiguration.getDouble("elytra-flight-event.speed-limit.max-speed", 75);
        this.disabledWorlds = this.fileConfiguration.getStringList("elytra-flight-event.disabled-worlds");
    }

    public final boolean getDeveloperModeIsEnabled() {
        return this.developerModeIsEnabled;
    }

    public final boolean getElytraSpeedLimitIsEnabled() {
        return this.elytraSpeedLimitIsEnabled;
    }

    public final boolean getDisableAllElytraFlight() {
        return disableAllElytraFlight;
    }

    public final List getDisabledWorlds() {
        return disabledWorlds;
    }

    public final double getElytraMaxSpeed() {
        return elytraMaxSpeed;
    }
}
