package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.handlers.EffectsHandler;
import org.bruno.elytraEssentials.handlers.FlightHandler;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ElytraFlightListener implements Listener
{
    private final FlightHandler flightHandler;
    private final StatsHandler statsHandler;
    private final EffectsHandler effectsHandler;

    public ElytraFlightListener(FlightHandler flightHandler, StatsHandler statsHandler, EffectsHandler effectsHandler){
        this.flightHandler = flightHandler;
        this.statsHandler = statsHandler;
        this.effectsHandler = effectsHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        statsHandler.loadPlayerStats(player);
        effectsHandler.loadPlayerActiveEffect(player);
        flightHandler.loadPlayerData(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        statsHandler.savePlayerStats(player);
        effectsHandler.clearPlayerActiveEffect(player);
        flightHandler.unloadPlayerData(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        statsHandler.setGliding(player, e.isGliding());
        if (e.isGliding()) {
            boolean shouldCancel = flightHandler.onGlideStartAttempt(player);
            if (shouldCancel) {
                e.setCancelled(true);
            }
        } else {
            flightHandler.handleGlideEnd(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        flightHandler.handlePlayerMove(e);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        flightHandler.handleVanillaMechanics(e);
    }
}