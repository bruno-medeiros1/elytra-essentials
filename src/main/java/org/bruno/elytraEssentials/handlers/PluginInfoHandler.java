package org.bruno.elytraEssentials.handlers;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bruno.elytraEssentials.helpers.VersionHelper;

public class PluginInfoHandler {
    private final String currentVersion;
    private String latestVersion;
    private boolean isUpdateAvailable;

    public PluginInfoHandler(PluginMeta pluginMeta) {
        this.currentVersion = pluginMeta.getVersion();
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
