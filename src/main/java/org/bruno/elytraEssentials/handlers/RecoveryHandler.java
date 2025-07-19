package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class RecoveryHandler
{
    private final ElytraEssentials plugin;

    private final FlightHandler flightHandler;
    private final MessagesHelper messagesHelper;

    private BukkitTask task;

    private final int recoveryAmount;
    private final boolean isNotifyOnRecoveryEnabled;

    public RecoveryHandler(ElytraEssentials plugin, FlightHandler flightHandler, MessagesHelper messagesHelper) {
        this.plugin = plugin;

        this.flightHandler = flightHandler;
        this.messagesHelper = messagesHelper;

        // Load config values once when the handler is created
        this.recoveryAmount = plugin.getConfigHandlerInstance().getRecoveryAmount();
        this.isNotifyOnRecoveryEnabled = plugin.getConfigHandlerInstance().getIsNotifyOnRecoveryEnabled();
    }

    public void start() {
        if (this.task != null && !this.task.isCancelled()) return;

        ConfigHandler config = plugin.getConfigHandlerInstance();
        if (!config.getIsTimeLimitEnabled() || !config.getIsRecoveryEnabled()) return;

        int recoveryInterval = config.getRecoveryInterval();

        // The task now runs on the main thread, which is safer for modifying player data.
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::recoverFlightTime, recoveryInterval * 20L, recoveryInterval * 20L);
    }

    public void cancel() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    /**
     * The core logic for the recovery task. Runs once per configured interval.
     */
    private void recoverFlightTime() {
        int maxTimeLimit = plugin.getConfigHandlerInstance().getMaxTimeLimit();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PermissionsHelper.PlayerBypassTimeLimit(player)) {
                continue;
            }

            UUID playerId = player.getUniqueId();
            int currentFlightTime = flightHandler.getCurrentFlightTime(playerId);

            // If a max time limit is set and the player is already at or above it, skip them.
            if (maxTimeLimit > 0 && currentFlightTime >= maxTimeLimit) {
                continue;
            }

            // Determine the actual amount of time to add, ensuring it doesn't exceed the max limit.
            int timeToAdd = this.recoveryAmount;
            if (maxTimeLimit > 0 && (currentFlightTime + timeToAdd) > maxTimeLimit) {
                timeToAdd = maxTimeLimit - currentFlightTime;
            }

            // If there's no time to add, skip to the next player.
            if (timeToAdd <= 0) {
                continue;
            }

            // Use the new, safer method in the flight listener to add time.
            flightHandler.addFlightTime(playerId, timeToAdd);

            // Send notification if enabled.
            if (isNotifyOnRecoveryEnabled) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.5f);
                String message = plugin.getMessagesHandlerInstance().getElytraFlightTimeRecovery()
                        .replace("{0}", TimeHelper.formatFlightTime(timeToAdd));
                messagesHelper.sendPlayerMessage(player, message);
            }
        }
    }
}
