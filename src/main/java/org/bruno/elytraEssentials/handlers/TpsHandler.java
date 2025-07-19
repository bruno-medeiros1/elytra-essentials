package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class TpsHandler {
    private final ElytraEssentials plugin;
    private final FoliaHelper foliaHelper;
    private final MessagesHelper messagesHelper;

    private static final int LOW_TPS_SECONDS_THRESHOLD = 10;
    private static final double LOW_TPS_THRESHOLD = 18.0;
    private static final double HEALTHY_TPS_THRESHOLD = 19.5; // Slightly higher for recovery

    private boolean isLagProtectionActive = false;
    private CancellableTask task;
    private int consecutiveLowTpsSeconds = 0;

    public TpsHandler(ElytraEssentials plugin, FoliaHelper foliaHelper, MessagesHelper messagesHelper) {
        this.plugin = plugin;
        this.foliaHelper = foliaHelper;
        this.messagesHelper = messagesHelper;
    }

    public void start() {
        if (this.task != null) return;

        // Run this check once per second (20 ticks), not every tick.
        this.task = foliaHelper.runTaskTimerGlobal(this::runTpsCheck, 100L, 20L);
    }

    public void cancel() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    /**
     * This method runs once per second to check TPS and update the lag state.
     */
    private void runTpsCheck() {
        // Get the server's 1-minute average TPS from the Paper API.
        double currentTps = Bukkit.getServer().getTPS()[0];

        if (currentTps < LOW_TPS_THRESHOLD) {
            consecutiveLowTpsSeconds++;
            if (consecutiveLowTpsSeconds >= LOW_TPS_SECONDS_THRESHOLD && !this.isLagProtectionActive) {
                this.isLagProtectionActive = true;
                messagesHelper.sendConsoleMessage("Server TPS is low (" + String.format("%.1f", currentTps) + "). Disabling elytra effects.");
            }
        } else {
            consecutiveLowTpsSeconds = 0;
            if (this.isLagProtectionActive && currentTps >= HEALTHY_TPS_THRESHOLD) {
                this.isLagProtectionActive = false;
                messagesHelper.sendConsoleMessage("Server TPS has recovered (" + String.format("%.1f", currentTps) + "). Re-enabling elytra effects.");
            }
        }
    }

    public boolean isLagProtectionActive() {
        return this.isLagProtectionActive;
    }

    public double getTps() {
        // Return the live TPS from the server
        return Bukkit.getServer().getTPS()[0];
    }
}