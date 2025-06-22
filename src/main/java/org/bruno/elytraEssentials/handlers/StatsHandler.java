package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsHandler {

    private final ElytraEssentials plugin;

    // Use a ConcurrentHashMap for thread safety with async saving
    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();
    private final Set<UUID> glidingPlayers = new HashSet<>();
    private BukkitTask task;

    public StatsHandler(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    // --- Cache Management ---
    public void loadPlayerStats(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PlayerStats stats = plugin.getDatabaseHandler().getPlayerStats(player.getUniqueId());
                    statsCache.put(player.getUniqueId(), stats);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Could not load stats for player " + player.getName());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void savePlayerStats(Player player) {
        PlayerStats stats = statsCache.get(player.getUniqueId());
        if (stats != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        plugin.getDatabaseHandler().savePlayerStats(stats);
                        plugin.getLogger().info("Stats Saved");

                    } catch (SQLException e) {
                        plugin.getLogger().severe("Could not save stats for player " + player.getName());
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
        // Remove from cache after saving
        statsCache.remove(player.getUniqueId());
        glidingPlayers.remove(player.getUniqueId());
    }

    public PlayerStats getStats(Player player) {
        // Return a default object if the player's stats aren't loaded yet
        return statsCache.getOrDefault(player.getUniqueId(), new PlayerStats(player.getUniqueId()));
    }

    // --- Gliding State ---
    public void setGliding(Player player, boolean isGliding) {
        if (isGliding) {
            glidingPlayers.add(player.getUniqueId());
        } else {
            glidingPlayers.remove(player.getUniqueId());
        }
    }

    public void start() {
        if (this.task != null)
            return;

        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::glidingTimeTracker, 20L, 20L);
    }

    public void cancel() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    public void glidingTimeTracker() {
        for (UUID uuid : glidingPlayers) {
            PlayerStats stats = statsCache.get(uuid);
            if (stats != null) {
                stats.addTime(1); // Add 1 second to their total time
            }
        }
    }
}
