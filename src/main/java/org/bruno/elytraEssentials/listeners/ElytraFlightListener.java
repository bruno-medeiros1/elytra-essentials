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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;

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
    private static final double SPEED_SLOW_THRESHOLD = 50.0;
    private static final double SPEED_NORMAL_THRESHOLD = 125.0;
    private static final double SPEED_FAST_THRESHOLD = 180;

    private final ElytraEssentials elytraEssentials;

    //  config values
    private double maxSpeed;
    private double maxSpeedBlocksPerTick;

    private boolean isElytraFlightDisabled;
    private boolean isElytraSpeedLimited;

    private List disabledElytraWorlds;
    private HashMap<String, Double> perWorldSpeedLimits;

    public ElytraFlightListener(ElytraEssentials elytraEssentials) {
        this.elytraEssentials = elytraEssentials;
        AssignConfigVariables();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerGlide(EntityToggleGlideEvent e) {
        Player player = (Player) e.getEntity();
        String playerWorld = player.getWorld().getName();

        if (this.isElytraFlightDisabled) {
            MessagesHelper.sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraDisabledMessage());
            e.setCancelled(true);
            return;
        }

        if (this.disabledElytraWorlds != null && this.disabledElytraWorlds.contains(playerWorld)) {
            MessagesHelper.sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraWorldDisabledMessage());
            e.setCancelled(true);
            return;
        }

        //  player is contained in a speed limited world
        if (this.perWorldSpeedLimits != null && this.perWorldSpeedLimits.containsKey(playerWorld)) {
            this.maxSpeed = perWorldSpeedLimits.get(playerWorld);
            this.maxSpeedBlocksPerTick = this.maxSpeed / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.isGliding())
            return;

        // Calculate speed (convert velocity magnitude to km/h)
        Vector velocity = player.getVelocity();
        double speed = velocity.length() * TICKS_IN_ONE_SECOND  * METERS_PER_SECOND_TO_KMH;

        String color = CalculateSpeedColor(speed);

        boolean playerBypassSpeedLimit = PlayerBypassSpeedLimit(player);

        if (!playerBypassSpeedLimit && this.isElytraSpeedLimited && speed > this.maxSpeed)
        {
            Bukkit.getLogger().info("Player " + player.getName() + " exceeded max speed: " + this.maxSpeed + " km/h");
            color = CalculateSpeedColor(this.maxSpeed);

            // Snap velocity to max speed
            Vector snappedVelocity = velocity.normalize().multiply(this.maxSpeedBlocksPerTick);
            player.setVelocity(snappedVelocity);

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacy("§eSpeed: " + color + String.format("%.2f", this.maxSpeed) + " §ekm/h"));
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacy("§eSpeed: " + color + String.format("%.2f", speed) + " §ekm/h"));
        }
    }

    private boolean PlayerBypassSpeedLimit(Player player) {
        return player.hasPermission("elytraessentials.bypass.speed.limit") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                player.hasPermission("elytraessentials.*");
    }

    private String CalculateSpeedColor(double speed) {
        if (speed > 0 && speed <= SPEED_SLOW_THRESHOLD)
            return "§a";
        if (speed > SPEED_SLOW_THRESHOLD && speed <= SPEED_NORMAL_THRESHOLD)
            return "§6";
        if (speed > SPEED_NORMAL_THRESHOLD && speed <= SPEED_FAST_THRESHOLD)
            return "§c";
        if (speed > SPEED_FAST_THRESHOLD)
            return "§4";
        else
            return "§7";
    }

    public void AssignConfigVariables() {
        ConfigHandler configHandler = elytraEssentials.getConfigHandlerInstance();

        Bukkit.getLogger().info("Assigning config values for ElytraFlightListener ");

        this.maxSpeed = configHandler.getDefaultMaxSpeed();
        this.maxSpeedBlocksPerTick = this.maxSpeed / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND;

        this.isElytraFlightDisabled = configHandler.getDisableAllElytraFlight();
        this.isElytraSpeedLimited = configHandler.getElytraSpeedLimitIsEnabled();

        this.disabledElytraWorlds = configHandler.getDisabledWorlds();

        this.perWorldSpeedLimits = configHandler.getPerWorldSpeedLimits();
    }
}