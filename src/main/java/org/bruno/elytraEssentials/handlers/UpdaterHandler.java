package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.net.URL;

public class UpdaterHandler {
    private final ElytraEssentials plugin;
    private final int resourceId;

    public UpdaterHandler(ElytraEssentials plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                consumer.accept(reader.readLine());
            } catch (IOException exception) {
                this.plugin.getLogger().warning("Cannot check for updates: " + exception.getMessage());
            }
        });
    }
}