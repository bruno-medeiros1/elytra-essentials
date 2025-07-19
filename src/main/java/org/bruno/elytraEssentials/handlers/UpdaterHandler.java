package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.utils.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

public class UpdaterHandler {

    private final Logger logger;
    private final FoliaHelper foliaHelper;
    private final ConfigHandler configHandler;
    private final PluginInfoHandler pluginInfoHandler;

    public UpdaterHandler(Logger logger, FoliaHelper foliaHelper, ConfigHandler configHandler, PluginInfoHandler pluginInfoHandler) {
        this.logger = logger;
        this.foliaHelper = foliaHelper;
        this.configHandler = configHandler;
        this.pluginInfoHandler = pluginInfoHandler;
    }

    /**
     * The main entry point for the update check.
     * It checks the config and, if enabled, performs the update check asynchronously.
     */
    public void performCheck() {
        if (!configHandler.getIsCheckForUpdatesEnabled()) {
            return;
        }

        foliaHelper.runAsyncTask(() -> {
            try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + Constants.Integrations.SPIGOT_RESOURCE_ID).openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                final String latestVersion = reader.readLine();

                // Pass the result to the PluginInfoHandler safely on the main thread
                foliaHelper.runTaskOnMainThread(() -> {
                    pluginInfoHandler.setUpdateInfo(latestVersion);

                    // The handler can also be responsible for logging the result
                    if (pluginInfoHandler.isUpdateAvailable()) {
                        logger.warning("A new version (" + pluginInfoHandler.getLatestVersion() + ") is available!");
                    }
                });

            } catch (IOException exception) {
                logger.warning("Cannot check for updates: " + exception.getMessage());
            }
        });
    }
}