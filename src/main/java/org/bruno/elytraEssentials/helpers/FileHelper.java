package org.bruno.elytraEssentials.helpers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

/**
 * A helper class to manage all custom configuration files for the plugin.
 */
public final class FileHelper {
    private final ElytraEssentials plugin;

    private FileConfiguration messagesConfig;
    private FileConfiguration shopConfig;
    private FileConfiguration achievementsConfig;

    public FileHelper(ElytraEssentials plugin) {
        this.plugin = plugin;

        initialize();
    }

    /**
     * Initializes all custom configuration files.
     * This method should be called once from the main onEnable() method.
     */
    public void initialize() {
        plugin.getLogger().info("Loading custom configuration files...");
        this.messagesConfig = setupCustomFile(Constants.Files.MESSAGES);
        this.shopConfig = setupCustomFile(Constants.Files.SHOP);
        this.achievementsConfig = setupCustomFile(Constants.Files.ACHIEVEMENTS);
        plugin.getLogger().info("All custom configuration files loaded successfully.");
    }

    /**
     * A generic method to handle the setup of any custom .yml file.
     * It ensures the file exists (creating it from defaults if necessary) and returns its configuration.
     *
     * @param fileName The name of the file (e.g., "messages.yml").
     * @return The loaded FileConfiguration for that file.
     */
    private FileConfiguration setupCustomFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        // If the file doesn't exist, save the default from the JAR.
        if (!file.exists()) {
            plugin.getLogger().info("File not found: " + fileName + ". Creating from defaults.");
            plugin.saveResource(fileName, false);
        }

        // Load and return the configuration.
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getMessagesConfig() {
        return this.messagesConfig;
    }

    public FileConfiguration getShopConfig() {
        return this.shopConfig;
    }

    public FileConfiguration getAchievementsConfig() {
        return this.achievementsConfig;
    }
}