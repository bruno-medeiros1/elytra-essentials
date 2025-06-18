package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RecoveryHandler
{
    private final ElytraEssentials plugin;

    private int recoveryAmount;
    private int recoveryInterval;
    private boolean isNotifyOnRecoveryEnabled;


    public RecoveryHandler(ElytraEssentials plugin) {
        this.plugin = plugin;

        startRecoveryTask();
    }

    public void startRecoveryTask() {
        var config = plugin.getConfigHandlerInstance();
        if (!config.getIsTimeLimitEnabled() || !config.getIsRecoveryEnabled()) return;

        recoveryAmount = config.getRecoveryAmount();
        recoveryInterval = config.getRecoveryInterval();
        isNotifyOnRecoveryEnabled = config.getIsNotifyOnRecoveryEnabled();

        Bukkit.getScheduler().runTaskTimer(plugin, this::recoverFlightTime, recoveryInterval * 20L, recoveryInterval * 20L);
    }

    private void recoverFlightTime() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PermissionsHelper.PlayerBypassTimeLimit(player))
                return;

            UUID playerId = player.getUniqueId();

            // Add recovery time to the player
            int currentFlightTime = plugin.getElytraFlightListener().GetAllActiveFlights().getOrDefault(playerId, 0);
            this.plugin.getElytraFlightListener().UpdatePlayerFlightTime(playerId, currentFlightTime + recoveryAmount);

            if (isNotifyOnRecoveryEnabled) {
                String message = this.plugin.getMessagesHandlerInstance().getElytraFlightTimeRecovery().replace("{0}", TimeHelper.formatFlightTime(recoveryAmount));
                plugin.getMessagesHelper().sendPlayerMessage(player, message);
            }
        }
    }


}
