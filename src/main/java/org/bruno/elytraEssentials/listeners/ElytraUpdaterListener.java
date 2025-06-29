package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.UpdaterHandler;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ElytraUpdaterListener implements Listener {
    private final ElytraEssentials plugin;
    private final UpdaterHandler updaterHandler;

    public ElytraUpdaterListener(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.updaterHandler = new UpdaterHandler(plugin, 126002);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (PermissionsHelper.hasUpdateNotifyPermission(player) && plugin.newerVersion) {
            player.sendMessage("§eA new version of ElytraEssentials is available!");
            player.sendMessage("§eDownload it here: https://www.spigotmc.org/resources/126002/");
        }
    }
}
