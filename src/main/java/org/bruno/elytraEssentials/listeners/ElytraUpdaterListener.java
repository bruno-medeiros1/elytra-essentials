package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ElytraUpdaterListener implements Listener {
    private final ElytraEssentials plugin;
    private final MessagesHelper messagesHelper;

    public ElytraUpdaterListener(ElytraEssentials plugin, MessagesHelper messagesHelper) {
        this.plugin = plugin;

        this.messagesHelper = messagesHelper;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean isCheckForUpdatesEnabled = plugin.getConfigHandlerInstance().getIsCheckForUpdatesEnabled();
        if (!isCheckForUpdatesEnabled) return;

        Player player = event.getPlayer();

        // Check for updates and send the interactive notification
        if (PermissionsHelper.hasUpdateNotifyPermission(player) && plugin.isNewerVersionAvailable) {
            messagesHelper.sendUpdateNotification(player, plugin.getLatestVersion());
        }
    }
}
