package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.FoliaHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.net.URL;

public class UpdaterHandler {
    private final ElytraEssentials plugin;
    private final FoliaHelper foliaHelper;
    private final int resourceId;

    public UpdaterHandler(ElytraEssentials plugin, FoliaHelper foliaHelper, int resourceId) {
        this.plugin = plugin;
        this.foliaHelper = foliaHelper;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        foliaHelper.runAsyncTask(() -> {
            try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                final String latestVersion = reader.readLine();

                foliaHelper.runTaskOnMainThread(() -> {
                    consumer.accept(latestVersion);
                });

            } catch (IOException exception) {
                this.plugin.getLogger().warning("Cannot check for updates: " + exception.getMessage());
            }
        });
    }
}