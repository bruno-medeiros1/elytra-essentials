package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ElytraUpdaterListener implements Listener {
    private final ElytraEssentials plugin;
    private final MessagesHelper messagesHelper;
    private final String latestVersion;
    private final ConfigHandler configHandler;

    public ElytraUpdaterListener(ElytraEssentials plugin, MessagesHelper messagesHelper, String latestVersion, ConfigHandler configHandler) {
        this.plugin = plugin;

        this.messagesHelper = messagesHelper;
        this.latestVersion = latestVersion;
        this.configHandler = configHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean isCheckForUpdatesEnabled = configHandler.getIsCheckForUpdatesEnabled();
        if (!isCheckForUpdatesEnabled) return;

        Player player = event.getPlayer();

        // Check for updates and send the interactive notification
        if (PermissionsHelper.hasUpdateNotifyPermission(player) && plugin.isNewerVersionAvailable) {
            messagesHelper.sendUpdateNotification(player, latestVersion);
        }
    }
}
