package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class TpsHandler {
    private final ElytraEssentials plugin;

    private static final double MAX_TPS = 20.0;
    private static final int LOW_TPS_SECONDS_THRESHOLD = 10;
    private static final double LOW_TPS_THRESHOLD = 18;
    private static final double HEALTHY_TPS_THRESHOLD = 19;

    private double tps;
    private boolean isLagProtectionActive = false;
    private BukkitTask task;

    //  fields for manual TPS calculation
    private long lastTickTime;
    private int tickCount;
    private int consecutiveLowTpsSeconds = 0;

    public TpsHandler(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.tps = MAX_TPS;
        this.lastTickTime = System.currentTimeMillis();
        this.tickCount = 0;
    }

    public void start() {
        if (this.task != null && !this.task.isCancelled()) {
            return;
        }

        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 1L);
    }

    public void cancel() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    /**
     * This method runs every server tick to count ticks for TPS calculation.
     */
    private void tick() {
        tickCount++;
        long now = System.currentTimeMillis();
        long elapsed = now - lastTickTime;

        // Check once per second (1000ms) has passed
        if (elapsed >= 1000) {
            // Calculate TPS: (ticks in the last second) / (time elapsed in seconds)
            this.tps = (double) tickCount * 1000 / elapsed;
            if (this.tps > MAX_TPS) {
                this.tps = MAX_TPS;
            }

            // Reset counters for the next second's calculation
            this.tickCount = 0;
            this.lastTickTime = now;

            // Now, run the lag protection logic with the newly calculated TPS
            runTpsCheck();
        }
    }

    /**
     * This method is now called internally once per second to update the lag state.
     */
    private void runTpsCheck() {
        if (this.tps < LOW_TPS_THRESHOLD) {
            consecutiveLowTpsSeconds++;
            if (consecutiveLowTpsSeconds >= LOW_TPS_SECONDS_THRESHOLD && !this.isLagProtectionActive) {
                this.isLagProtectionActive = true;
                plugin.getMessagesHelper().sendConsoleMessage("Server TPS has been below " + String.format("%.1f", LOW_TPS_THRESHOLD) + " for " + LOW_TPS_SECONDS_THRESHOLD + " seconds. Disabling elytra effects.");
            }
        } else {
            consecutiveLowTpsSeconds = 0;
            if (this.isLagProtectionActive && this.tps >= HEALTHY_TPS_THRESHOLD) {
                this.isLagProtectionActive = false;
                plugin.getMessagesHelper().sendConsoleMessage("Server TPS has recovered to " + String.format("%.1f", this.tps) + ". Re-enabling elytra effects.");
            }
        }
    }

    public boolean isLagProtectionActive() {
        return this.isLagProtectionActive;
    }

    public double getTps() {
        return this.tps;
    }
}