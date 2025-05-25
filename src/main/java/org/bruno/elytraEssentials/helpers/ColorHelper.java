package org.bruno.elytraEssentials.helpers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;

public class ColorHelper {
    private final ElytraEssentials elytraEssentials;

    private FileConfiguration fileConfiguration = null;
    private File file = null;

    public ColorHelper(ElytraEssentials elytraEssentials) {
        this.elytraEssentials = elytraEssentials;
        this.EnsureMessagesFileExists();
    }

    //TODO: Rever esta função
    public static String ParseColoredString(String input) {
        // Splitting the input string into an array, keeping the '&' delimiter in the result.
        // The regex splits the string while preserving the '&' by using lookbehind and lookahead.
        String[] stringArray = input.split(String.format("((?<=%1$s)|(?=%1$s))", "&"));

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < stringArray.length; ++i) {
            if (stringArray[i].equalsIgnoreCase("&")) {
                if (stringArray[++i].charAt(0) == '#') {
                    stringBuilder.append(net.md_5.bungee.api.ChatColor.of(stringArray[i].substring(0, 7)))
                            .append(stringArray[i].substring(7));
                    continue;
                }
                stringBuilder.append(ChatColor.translateAlternateColorCodes('&', "&" + stringArray[i]));
                continue;
            }
            // If the part is not '&', append it as is to the final string.
            stringBuilder.append(stringArray[i]);
        }

        // Return the fully processed string with color codes applied.
        return stringBuilder.toString();
    }

    public void InitializeMessagesFile() {
        if (this.file == null) {
            this.file = new File(this.elytraEssentials.getDataFolder(), "messages.yml");
        }

        // Load the configuration file
        this.fileConfiguration = YamlConfiguration.loadConfiguration(this.file);

        // Load default values from the plugin's resource if it exists
        InputStream inputStream = this.elytraEssentials.getResource("messages.yml");
        if (inputStream != null) {
            try (Reader reader = new InputStreamReader(inputStream)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                this.fileConfiguration.setDefaults(defaultConfig);
            } catch (IOException e) {
                e.printStackTrace();
                // Log the error or handle it as appropriate
            }
        }
    }

    public FileConfiguration GetFileConfiguration() {
        if (this.fileConfiguration == null) {
            this.InitializeMessagesFile();
        }
        return this.fileConfiguration;
    }

    public void EnsureMessagesFileExists() {
        if (this.file == null) {
            this.file = new File(this.elytraEssentials.getDataFolder(), "messages.yml");
        }
        if (!this.file.exists()) {
            this.elytraEssentials.saveResource("messages.yml", false);
        }
    }
}
