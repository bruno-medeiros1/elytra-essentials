package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
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

    private int recoveryAmount;
    private boolean isNotifyOnRecoveryEnabled;

    private BukkitTask task;

    public RecoveryHandler(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (this.task != null)
            return;

        var config = plugin.getConfigHandlerInstance();
        if (!config.getIsTimeLimitEnabled() || !config.getIsRecoveryEnabled())
            return;

        // Load config values once when the task starts
        int recoveryInterval = config.getRecoveryInterval();
        this.recoveryAmount = config.getRecoveryAmount();
        this.isNotifyOnRecoveryEnabled = config.getIsNotifyOnRecoveryEnabled();

        // Schedule the task and store it in our variable
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::recoverFlightTime, recoveryInterval * 20L, recoveryInterval * 20L);
    }

    public void cancel() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void recoverFlightTime() {
        var maxTimeLimit = plugin.getConfigHandlerInstance().getMaxTimeLimit();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PermissionsHelper.PlayerBypassTimeLimit(player))
                continue;

            UUID playerId = player.getUniqueId();

            // Add recovery time to the player
            int currentFlightTime = plugin.getElytraFlightListener().GetAllActiveFlights().getOrDefault(playerId, 0);

            //  we have a limit to check
            if (maxTimeLimit > 0) {
                if (currentFlightTime == maxTimeLimit)
                    continue;

                int total = currentFlightTime + recoveryAmount;
                if (total > maxTimeLimit){
                    total = maxTimeLimit;
                    recoveryAmount = maxTimeLimit - currentFlightTime;
                }

                this.plugin.getElytraFlightListener().UpdatePlayerFlightTime(playerId, total);

                if (isNotifyOnRecoveryEnabled)
                {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.5f);
                    String message = this.plugin.getMessagesHandlerInstance().getElytraFlightTimeRecovery().replace("{0}", TimeHelper.formatFlightTime(recoveryAmount));
                    plugin.getMessagesHelper().sendPlayerMessage(player, message);
                }
            }
            else {
                this.plugin.getElytraFlightListener().UpdatePlayerFlightTime(playerId, currentFlightTime + recoveryAmount);

                if (isNotifyOnRecoveryEnabled) {
                    String message = this.plugin.getMessagesHandlerInstance().getElytraFlightTimeRecovery().replace("{0}", TimeHelper.formatFlightTime(recoveryAmount));
                    plugin.getMessagesHelper().sendPlayerMessage(player, message);
                }
            }
        }
    }
}
