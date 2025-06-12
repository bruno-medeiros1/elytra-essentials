package org.bruno.elytraEssentials.handlers;

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
    private boolean isCheckForUpdatesEnabled;

    // Flight section
    private boolean isGlobalFlightDisabled;
    private List<String> disabledWorlds;
    private boolean isSpeedLimitEnabled;
    private double defaultSpeedLimit;
    private HashMap<String, Double> perWorldSpeedLimits;
    private boolean isTimeLimitEnabled;
    private int defaultTimeLimit;
    private HashMap<String, Integer> perWorldTimeLimits;

    //  Database section
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    public ConfigHandler(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
        SetConfigVariables();
    }

    public final void SetConfigVariables() {
        this.isElytraEquipDisabled = this.fileConfiguration.getBoolean("general.disable-elytra-equipment", false);
        this.isDebugModeEnabled = this.fileConfiguration.getBoolean("general.debug-mode", true);
        this.isCheckForUpdatesEnabled = this.fileConfiguration.getBoolean("general.check-for-updates", true);

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
                    Bukkit.getLogger().info("World Speed Limit for " + worldName + " : " + worldSpeedLimit);
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
        this.defaultTimeLimit = this.fileConfiguration.getInt("flight.time-limit.default", 30);

        ConfigurationSection perWorldTimeLimitSection = this.fileConfiguration.getConfigurationSection("flight.time-limit.per-world");
        this.perWorldTimeLimits = new HashMap<>();

        if (perWorldTimeLimitSection != null) {
            for (String worldName : perWorldTimeLimitSection.getKeys(false)) {
                try {
                    int worldTimeLimit = perWorldTimeLimitSection.getInt(worldName, this.defaultTimeLimit);
                    Bukkit.getLogger().info("World Time Limit for " + worldName + " : " + worldTimeLimit);
                    this.perWorldTimeLimits.put(worldName, worldTimeLimit);
                } catch (Exception e) {
                    Bukkit.getLogger().info("Invalid time limit for world '" + worldName + "' in config.yml. Using default speed limit.");
                    this.perWorldTimeLimits.put(worldName, this.defaultTimeLimit);
                }
            }
        } else {
            Bukkit.getLogger().info("No per-world time limits defined in config.yml. Using default values.");
        }

        this.host = this.fileConfiguration.getString("flight.time-limit.database.host", "localhost");
        this.port = this.fileConfiguration.getInt("flight.time-limit.database.port", 3306);
        this.database = this.fileConfiguration.getString("flight.time-limit.database.database", "elytraessentials");
        this.username = this.fileConfiguration.getString("flight.time-limit.database.user", "root");
        this.password = this.fileConfiguration.getString("flight.time-limit.database.password", "");
    }

    public final boolean getIsElytraEquipDisabled() { return this.isElytraEquipDisabled; }
    public final boolean getIsDebugModeEnabled() {
        return this.isDebugModeEnabled;
    }
    public final boolean getIsCheckForUpdatesEnabled() { return this.isCheckForUpdatesEnabled; }

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

    public String getHost() { return this.host; }
    public int getPort() { return this.port; }
    public String getDatabase() { return this.database; }
    public String getUsername() { return this.username; }
    public String getPassword() { return this.password; }
}
