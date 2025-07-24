package org.bruno.elytraEssentials.handlers;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bruno.elytraEssentials.helpers.VersionHelper;
import org.bukkit.plugin.PluginDescriptionFile;

public class PluginInfoHandler {
    private final String currentVersion;
    private String latestVersion;
    private boolean isUpdateAvailable;

    // Modern constructor (for Paper 1.19+)
    public PluginInfoHandler(PluginMeta pluginMeta) {
        this.currentVersion = pluginMeta.getVersion();
        this.latestVersion = this.currentVersion;
        this.isUpdateAvailable = false;
    }

    // Legacy constructor (for Spigot and Paper < 1.19)
    public PluginInfoHandler(PluginDescriptionFile descriptionFile) {
        this.currentVersion = descriptionFile.getVersion();
        this.latestVersion = this.currentVersion;
        this.isUpdateAvailable = false;
    }

    /**
     * Called by the asynchronous update checker to provide the latest version found online.
     * This method is synchronized to ensure thread safety.
     *
     * @param latestVersion The version string fetched from the update server.
     */
    public synchronized void setUpdateInfo(String latestVersion) {
        this.latestVersion = latestVersion;
        this.isUpdateAvailable = VersionHelper.isNewerVersion(this.currentVersion, this.latestVersion);
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public synchronized String getLatestVersion() {
        return latestVersion;
    }

    public synchronized boolean isUpdateAvailable() {
        return isUpdateAvailable;
    }
}
