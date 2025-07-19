package org.bruno.elytraEssentials.helpers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public final class FoliaHelper {
    private final ElytraEssentials plugin;
    private final boolean isFolia;

    /**
     * Constructs the FoliaHelper and performs a one-time check to detect if the
     * server is running on Folia.
     *
     * @param plugin The main plugin instance, required for scheduling tasks.
     */
    public FoliaHelper(ElytraEssentials plugin) {
        this.plugin = plugin;
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            // Not Folia
        }
        this.isFolia = folia;
    }

    /**
     * Checks if the server is running on Folia.
     *
     * @return {@code true} if the server is a Folia instance, {@code false} otherwise.
     */
    public boolean isFolia() {
        return isFolia;
    }

    /**
     * Executes a task on the appropriate thread for a specific entity.
     * On Folia, this uses the entity's personal scheduler to run on its regional thread.
     * On Spigot/Paper, this uses the main server thread.
     *
     * @param entity The entity to associate the task with.
     * @param task   The Runnable to execute.
     * @return A {@link CancellableTask} wrapper for the scheduled task.
     */
    public CancellableTask runTask(Entity entity, Runnable task) {
        if (isFolia) {
            return new CancellableTask(entity.getScheduler().run(plugin, scheduledTask -> task.run(), null));
        } else {
            return new CancellableTask(plugin.getServer().getScheduler().runTask(plugin, task));
        }
    }

    /**
     * Runs a repeating task tied to a specific entity's thread.
     * On Folia, this ensures the task runs on the correct regional thread for the entity.
     * On Spigot/Paper, this runs on the main server thread.
     *
     * @param entity The entity to associate the task with.
     * @param task   The Runnable to execute repeatedly.
     * @param delay  The initial delay in server ticks.
     * @param period The period in server ticks between executions.
     * @return A {@link CancellableTask} wrapper for the scheduled task.
     */
    public CancellableTask runTaskTimerForEntity(Entity entity, Runnable task, long delay, long period) {
        if (isFolia) {
            return new CancellableTask(entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), null, delay, period));
        } else {
            return new CancellableTask(plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period));
        }
    }

    /**
     * Executes a task on the appropriate thread for a specific entity after a delay.
     *
     * @param entity The entity to associate the task with.
     * @param task   The Runnable to execute.
     * @param delay  The delay in server ticks before execution.
     * @return A {@link CancellableTask} wrapper for the scheduled task.
     */
    public CancellableTask runTaskLater(Entity entity, Runnable task, long delay) {
        if (isFolia) {
            return new CancellableTask(entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delay));
        } else {
            return new CancellableTask(plugin.getServer().getScheduler().runTaskLater(plugin, task, delay));
        }
    }

    /**
     * Runs a repeating task on the main global server thread.
     * On Folia, this uses the {@code GlobalRegionScheduler}.
     * On Spigot/Paper, this uses the standard Bukkit scheduler.
     * Ideal for global processes not tied to a specific entity or location.
     *
     * @param task   The Runnable to execute repeatedly.
     * @param delay  The initial delay in server ticks.
     * @param period The period in server ticks between executions.
     * @return A {@link CancellableTask} wrapper for the scheduled task.
     */
    public CancellableTask runTaskTimerGlobal(Runnable task, long delay, long period) {
        if (isFolia) {
            return new CancellableTask(plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delay, period));
        } else {
            return new CancellableTask(plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period));
        }
    }

    /**
     * Runs a task asynchronously on a background thread pool.
     * Ideal for long-running operations like database queries or web requests.
     *
     * @param task The Runnable to execute asynchronously.
     * @return A {@link CancellableTask} wrapper for the scheduled task.
     */
    public CancellableTask runAsyncTask(Runnable task) {
        if (isFolia) {
            return new CancellableTask(Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run()));
        } else {
            return new CancellableTask(plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task));
        }
    }

    /**
     * Executes a task on the main server thread from any context (including async).
     * This is the safe way to pass results from a background thread back to the game.
     * On Folia, this uses the Global Region Scheduler.
     * On Spigot/Paper, this uses the standard Bukkit Scheduler.
     *
     * @param task The task to run on the main thread.
     * @return A {@link CancellableTask} wrapper for the scheduled task.
     */
    public void runTaskOnMainThread(Runnable task) {
        if (isFolia) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }
}