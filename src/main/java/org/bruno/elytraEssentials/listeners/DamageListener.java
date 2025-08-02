package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.handlers.ArmoredElytraHandler;
import org.bruno.elytraEssentials.handlers.FlightHandler;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.handlers.UpgradeHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class DamageListener implements Listener {

    private final FlightHandler flightHandler;
    private final StatsHandler statsHandler;
    private final ArmoredElytraHandler armoredElytraHandler;
    private final UpgradeHandler upgradeHandler;

    private final Random random = new Random();

    public DamageListener(FlightHandler flightHandler, StatsHandler statsHandler, ArmoredElytraHandler armoredElytraHandler,
                          UpgradeHandler upgradeHandler) {
        this.flightHandler = flightHandler;
        this.statsHandler = statsHandler;
        this.armoredElytraHandler = armoredElytraHandler;
        this.upgradeHandler = upgradeHandler;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Handle Kinetic Energy Protection & Kinetic Resistance Upgrade
        if (event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            // First, check if the global protection is enabled in the config.
            if (flightHandler.isKineticProtectionEnabled()) {
                event.setCancelled(true);
                statsHandler.getStats(player).incrementPluginSaves();
                return;
            }

            // If global protection is off, check for the upgrade.
            ItemStack chestplate = player.getInventory().getChestplate();
            double resistanceChance = upgradeHandler.getKineticResistanceChance(chestplate);

            // If the player has the upgrade, roll the dice to see if damage is negated.
            if (resistanceChance > 0 && random.nextDouble() * 100 < resistanceChance) {
                event.setCancelled(true);
                statsHandler.getStats(player).incrementPluginSaves();
                return;
            }
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