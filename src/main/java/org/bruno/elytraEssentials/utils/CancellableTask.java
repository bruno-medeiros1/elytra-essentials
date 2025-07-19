package org.bruno.elytraEssentials.utils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

/**
 * A wrapper class designed to handle the incompatibility between Bukkit/Spigot tasks
 * ({@link BukkitTask}) and Folia's modern {@link ScheduledTask}.
 * <p>
 * Since Folia's {@code ScheduledTask} does not extend {@code BukkitTask}, a direct
 * cast is not possible. This class acts as a universal container, holding either
 * type of task object.
 * <p>
 * It provides a single, platform-agnostic {@link #cancel()} method, allowing the rest
 * of the plugin to manage tasks without needing to know whether the server is running
 * on Folia or a traditional platform.
 */
public class CancellableTask {
    private final Object task;
    private final boolean isFoliaTask;

    public CancellableTask(BukkitTask bukkitTask) {
        this.task = bukkitTask;
        this.isFoliaTask = false;
    }

    public CancellableTask(ScheduledTask foliaTask) {
        this.task = foliaTask;
        this.isFoliaTask = true;
    }

    public void cancel() {
        if (isFoliaTask) {
            ((ScheduledTask) this.task).cancel();
        } else {
            ((BukkitTask) this.task).cancel();
        }
    }
}
