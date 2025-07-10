package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ElytraUpdaterListener implements Listener {
    private final ElytraEssentials plugin;

    public ElytraUpdaterListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean isCheckForUpdatesEnabled = plugin.getConfigHandlerInstance().getIsCheckForUpdatesEnabled();
        if (!isCheckForUpdatesEnabled) return;

        Player player = event.getPlayer();

        // Check for updates and send the interactive notification
        if (PermissionsHelper.hasUpdateNotifyPermission(player) && plugin.isNewerVersionAvailable) {
            plugin.getMessagesHelper().sendUpdateNotification(player, plugin.getLatestVersion());
        }
    }
}
