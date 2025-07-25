package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.handlers.TandemHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class TandemListener implements Listener {

    private final TandemHandler tandemHandler;

    public TandemListener(TandemHandler tandemHandler) {
        this.tandemHandler = tandemHandler;
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // If a passenger starts sneaking, they want to dismount.
        if (event.isSneaking() && tandemHandler.isPassenger(player)) {
            tandemHandler.dismountPassenger(player, null, true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tandemHandler.clearPlayerData(event.getPlayer());
    }
}