package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RecoveryHandler
{
    private final FlightHandler flightHandler;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler; // For getting message strings
    private final ConfigHandler configHandler;
    private final FoliaHelper foliaHelper;

    private CancellableTask task;

    public RecoveryHandler(FlightHandler flightHandler, MessagesHelper messagesHelper, MessagesHandler messagesHandler, ConfigHandler configHandler, FoliaHelper foliaHelper) {
        this.flightHandler = flightHandler;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;
        this.configHandler = configHandler;
        this.foliaHelper = foliaHelper;
    }

    public void start() {
        if (this.task != null) return;
        if (!configHandler.getIsTimeLimitEnabled() || !configHandler.getIsRecoveryEnabled()) return;

        int recoveryInterval = configHandler.getRecoveryInterval();

        // Use the Folia-safe global timer to run the recovery task
        this.task = foliaHelper.runTaskTimerGlobal(this::recoverFlightTime, recoveryInterval * 20L, recoveryInterval * 20L);
    }

    public void shutdown() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    /**
     * The core logic for the recovery task. Runs once per configured interval.
     */
    private void recoverFlightTime() {
        int maxTimeLimit = configHandler.getMaxTimeLimit();
        int timeToAddBase = configHandler.getRecoveryAmount();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PermissionsHelper.PlayerBypassTimeLimit(player)) continue;

            UUID playerId = player.getUniqueId();
            int currentFlightTime = flightHandler.getCurrentFlightTime(playerId);

            if (maxTimeLimit > 0 && currentFlightTime >= maxTimeLimit) continue;

            int timeToAdd = timeToAddBase;
            if (maxTimeLimit > 0 && (currentFlightTime + timeToAdd) > maxTimeLimit) {
                timeToAdd = maxTimeLimit - currentFlightTime;
            }

            if (timeToAdd <= 0) continue;

            flightHandler.addFlightTime(playerId, timeToAdd, player);
        }
    }
}
