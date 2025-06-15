package org.bruno.elytraEssentials.helpers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

//  TODO: Improve class methods also implement logHelper for improved logging
public class FileHelper {
    private final ElytraEssentials plugin;

    private FileConfiguration messagesFileConfiguration = null;
    private File messagesFile = null;

    private FileConfiguration shopFileConfiguration = null;
    private File shopFile = null;


    public FileHelper(ElytraEssentials plugin) {
        this.plugin = plugin;

        EnsureMessagesFileExists();
        EnsureShopFileExists();
    }

    public void InitializeMessagesFile() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }

        //TODO: Add try catch block here maybe
        // Load the configuration file
        messagesFileConfiguration= YamlConfiguration.loadConfiguration(messagesFile);

        // Load default values from the plugin's resource if it exists
        InputStream inputStream = plugin.getResource("messages.yml");
        if (inputStream != null) {
            try (Reader reader = new InputStreamReader(inputStream)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                messagesFileConfiguration.setDefaults(defaultConfig);
            } catch (IOException e) {
                Bukkit.getLogger().severe("Failed to read messages.yml");
                Bukkit.getLogger().severe("Error: " + e.getMessage());
                Bukkit.getLogger().severe("Stack Trace:");
                for (StackTraceElement element : e.getStackTrace()) {
                    Bukkit.getLogger().severe("  at " + element.toString());
                }
            }
        }
    }

    public void InitializeShopFile() {
        if (shopFile == null) {
            shopFile = new File(plugin.getDataFolder(), "shop.yml");
        }

        //TODO: Add try catch block here maybe
        // Load the configuration file
        shopFileConfiguration = YamlConfiguration.loadConfiguration(shopFile);

        // Load default values from the plugin's resource if it exists
        InputStream inputStream = plugin.getResource("shop.yml");
        if (inputStream != null) {
            try (Reader reader = new InputStreamReader(inputStream)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                shopFileConfiguration.setDefaults(defaultConfig);
            } catch (IOException e) {
                Bukkit.getLogger().severe("Failed to read shop.yml");
                Bukkit.getLogger().severe("Error: " + e.getMessage());
                Bukkit.getLogger().severe("Stack Trace:");
                for (StackTraceElement element : e.getStackTrace()) {
                    Bukkit.getLogger().severe("  at " + element.toString());
                }
            }
        }
    }

    public FileConfiguration GetMessagesFileConfiguration() {
        if (messagesFileConfiguration == null) {
            this.InitializeMessagesFile();
        }
        return messagesFileConfiguration;
    }

    public FileConfiguration GetShopFileConfiguration() {
        if (shopFileConfiguration == null) {
            this.InitializeShopFile();
        }
        return shopFileConfiguration;
    }

    private void EnsureMessagesFileExists() {
        if (messagesFile == null) {
            Bukkit.getLogger().info("File does not exist, creating new messages.yml");
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    private void EnsureShopFileExists() {
        if (shopFile == null) {
            Bukkit.getLogger().info("File does not exist, creating new shop.yml");
            shopFile = new File(plugin.getDataFolder(), "shop.yml");
        }
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
    }
}