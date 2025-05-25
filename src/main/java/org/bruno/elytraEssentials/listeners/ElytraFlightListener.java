package org.bruno.elytraEssentials.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/// Vanilla Speed Elytra:
///
/// Without rockets: 36 km/h
/// With rockets (normal flight): 108 km/h
/// With rockets (steep dive): 180 km/h
///
public class ElytraFlightListener implements Listener
{
    private static final int TICKS_IN_ONE_SECOND = 20;
    private static final double METERS_PER_SECOND_TO_KMH = 3.6; // Conversion factor: 1 m/s = 3.6 km/h
    private static final double SPEED_GREEN_THRESHOLD = 50.0;
    private static final double SPEED_ORANGE_THRESHOLD = 125.0;
    private static final long CHECK_INTERVAL = 5L; // Every 5 ticks
    private static final float CHECK_INTERVAL_FACTOR = (float) TICKS_IN_ONE_SECOND / CHECK_INTERVAL;

    private final ElytraEssentials elytraEssentials;
    private final Set<UUID> monitoredPlayers = new HashSet<>();

    public ElytraFlightListener(ElytraEssentials elytraEssentials) {
        this.elytraEssentials = elytraEssentials;

        StartSpeedMonitorTask();
    }

    private void StartSpeedMonitorTask(){
        // Start a repeating task to monitor players
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<UUID> iterator = monitoredPlayers.iterator();

                while (iterator.hasNext()) {
                    Player player = Bukkit.getPlayer(iterator.next());
                    if (player == null || !player.isOnline() || !player.isGliding()) {
                        iterator.remove();
                        continue;
                    }

                    // TODO: Check if we can optimize this
                    double currentMaxSpeed = elytraEssentials.getConfigHandlerInstance().getElytraMaxSpeed();
                    double currentMaxSpeedBlocksPerTick = currentMaxSpeed / METERS_PER_SECOND_TO_KMH / 20;

                    Vector velocity = player.getVelocity();
                    double speed = velocity.length() * 20 * METERS_PER_SECOND_TO_KMH;
                    Bukkit.getLogger().info("Player " + player.getName() + " current speed: " + speed + " km/h");

                    // Handle speed limit enforcement
                    if (speed > currentMaxSpeed) {
                        String color = (speed < SPEED_GREEN_THRESHOLD) ? "§a" : (speed < SPEED_ORANGE_THRESHOLD) ? "§6" : "§c";
                        Bukkit.getLogger().info("Player " + player.getName() + " exceeded max speed: " + currentMaxSpeed + " km/h");

                        // Snap velocity to max speed
                        Vector snappedVelocity = velocity.normalize().multiply(currentMaxSpeedBlocksPerTick);
                        player.setVelocity(snappedVelocity);

                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacy("§eSpeed: " + color + String.format("%.2f", currentMaxSpeed) + " §ekm/h"));
                    } else {
                        String color = (speed < SPEED_GREEN_THRESHOLD) ? "§a" : (speed < SPEED_ORANGE_THRESHOLD) ? "§6" : "§c";
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacy("§eSpeed: " + color + String.format("%.2f", speed) + " §ekm/h"));
                    }
                }
            }
        }.runTaskTimer(this.elytraEssentials, 0L, CHECK_INTERVAL); // Start task, repeat every 5 ticks
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerGlide(EntityToggleGlideEvent e) {
        Player player = (Player) e.getEntity();

        if (shouldRemovePlayer(player)) {
            monitoredPlayers.remove(player.getUniqueId());
            return;
        }

        if (e.isGliding()) {
            monitoredPlayers.add(player.getUniqueId());
        } else {
            monitoredPlayers.remove(player.getUniqueId());
        }
    }

    private boolean shouldRemovePlayer(Player player) {
        ConfigHandler configHandler = elytraEssentials.getConfigHandlerInstance();

        if (configHandler.getDisableAllElytraFlight()) {
            MessagesHelper.sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraDisabledMessage());
            return true;
        }

        if (configHandler.getDisabledWorlds() != null && configHandler.getDisabledWorlds().contains(player.getWorld().getName())) {
            MessagesHelper.sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraWorldDisabledMessage());
            return true;
        }

        return !configHandler.getElytraSpeedLimitIsEnabled() ||
                player.hasPermission("elytraessentials.bypass.elytra") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                player.hasPermission("elytraessentials.*");
    }
}
