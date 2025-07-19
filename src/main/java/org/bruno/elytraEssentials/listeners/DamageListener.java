package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.handlers.ArmoredElytraHandler;
import org.bruno.elytraEssentials.handlers.FlightHandler;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener {

    private final FlightHandler flightHandler;
    private final StatsHandler statsHandler;
    private final ArmoredElytraHandler armoredElytraHandler; // New dependency

    public DamageListener(FlightHandler flightHandler, StatsHandler statsHandler, ArmoredElytraHandler armoredElytraHandler) {
        this.flightHandler = flightHandler;
        this.statsHandler = statsHandler;
        this.armoredElytraHandler = armoredElytraHandler;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Handle Kinetic Energy Protection
        if (flightHandler.isKineticProtectionEnabled() && event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            event.setCancelled(true);
            statsHandler.getStats(player).incrementPluginSaves();
            return;
        }

        // Handle Fall Damage Protection
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && flightHandler.isProtectedFromFallDamage(player.getUniqueId())) {
            flightHandler.removeFallProtection(player.getUniqueId());
            event.setCancelled(true);
            statsHandler.getStats(player).incrementPluginSaves();
            return;
        }

        armoredElytraHandler.handleDamage(event);
    }
}