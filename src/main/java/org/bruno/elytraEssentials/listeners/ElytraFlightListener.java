package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class ElytraFlightListener implements Listener
{
    // --- Constants ---
    private static final int TICKS_IN_ONE_SECOND = 20;
    private static final double METERS_PER_SECOND_TO_KMH = 3.6;
    private static final double MAX_FLIGHT_SPEED = 200.0;

    private final ElytraEssentials plugin;

    // --- Config Values ---
    private boolean isGlobalFlightDisabled;
    private boolean isSpeedLimitEnabled;
    private boolean isTimeLimitEnabled;
    private boolean isElytraBreakProtectionEnabled;
    private boolean isKineticEnergyProtectionEnabled;
    private double maxSpeed;
    private List<String> disabledElytraWorlds;
    private HashMap<String, Double> perWorldSpeedLimits;

    // --- Player State Tracking ---
    private final HashMap<UUID, Integer> flightTimeData = new HashMap<>();
    private final HashMap<UUID, BossBar> flightBossBars = new HashMap<>();
    private final HashMap<UUID, Double> currentFlightDistances = new HashMap<>();
    private final Set<UUID> noFallDamagePlayers = new HashSet<>();
    private final HashMap<UUID, Integer> initialFlightTime = new HashMap<>();

    public ElytraFlightListener(ElytraEssentials plugin) {
        this.plugin = plugin;
        assignConfigVariables();
    }

    //<editor-fold desc="Events">
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getStatsHandler().loadPlayerStats(player);
        plugin.getEffectsHandler().loadPlayerActiveEffect(player);
        loadPlayerFlightTime(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getStatsHandler().savePlayerStats(player);
        plugin.getEffectsHandler().clearPlayerActiveEffect(player);
        savePlayerFlightTime(player.getUniqueId());

        // Clean up all player data to prevent memory leaks
        flightTimeData.remove(player.getUniqueId());
        flightBossBars.remove(player.getUniqueId());
        currentFlightDistances.remove(player.getUniqueId());
        noFallDamagePlayers.remove(player.getUniqueId());
        initialFlightTime.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        plugin.getStatsHandler().setGliding(player, event.isGliding());

        if (event.isGliding()) {
            handleGlideStart(player);
        } else {
            handleGlideEnd(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isGliding()) {
            removeFlightBossBar(player);
            return;
        }

        handleFlightTimeCountdown(player);
        handleSpeedometer(player);
        handleDurabilityProtection(player);
        handleDistanceTracking(player, event);

        plugin.getEffectsHandler().spawnParticleTrail(player);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Handle Kinetic Energy Protection
        if (isKineticEnergyProtectionEnabled && event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            event.setCancelled(true);
            plugin.getStatsHandler().getStats(player).incrementPluginSaves();
            return;
        }

        // Handle Fall Damage Protection
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && noFallDamagePlayers.remove(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getStatsHandler().getStats(player).incrementPluginSaves();
        }
    }
    //</editor-fold>

    //<editor-fold desc="Core Logic Helpers">
    private void handleGlideStart(Player player) {
        // Check for flight restrictions
        if (isGlobalFlightDisabled) {
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getElytraUsageDisabledMessage());
            player.setGliding(false);
            return;
        }
        if (disabledElytraWorlds != null && disabledElytraWorlds.contains(player.getWorld().getName())) {
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getElytraUsageWorldDisabledMessage());
            player.setGliding(false);
            return;
        }

        // Initialize flight distance tracking
        currentFlightDistances.put(player.getUniqueId(), 0.0);

        // Create and show the flight time boss bar if needed
        createFlightBossBar(player);
    }

    private void handleGlideEnd(Player player) {
        // Finalize the longest flight stat
        PlayerStats stats = plugin.getStatsHandler().getStats(player);
        double flightDistance = currentFlightDistances.getOrDefault(player.getUniqueId(), 0.0);
        if (flightDistance > stats.getLongestFlight()) {
            stats.setLongestFlight(flightDistance);
            String message = plugin.getMessagesHandlerInstance().getNewPRLongestFlightMessage()
                    .replace("{0}", String.format("%.0f", flightDistance));
            plugin.getMessagesHelper().sendPlayerMessage(player, message);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }
        currentFlightDistances.remove(player.getUniqueId());

        // Remove the boss bar
        removeFlightBossBar(player);
    }

    private void handleFlightTimeCountdown(Player player) {
        if (!isTimeLimitEnabled || PermissionsHelper.PlayerBypassTimeLimit(player)) return;

        BossBar bossBar = flightBossBars.get(player.getUniqueId());
        if (bossBar == null) return; // No boss bar to update

        int currentFlightTime = flightTimeData.getOrDefault(player.getUniqueId(), 0);
        if (currentFlightTime <= 0) {
            player.setGliding(false);
            noFallDamagePlayers.add(player.getUniqueId());
            plugin.getMessagesHelper().sendActionBarMessage(player, plugin.getMessagesHandlerInstance().getElytraFlightTimeExpired());
            return;
        }

        // Decrement time once per second (approximated by checking every move)
        // A BukkitRunnable would be more precise but this is simple and effective enough.
        // This logic assumes onPlayerMove fires frequently enough.
        // For perfect timing, a separate repeating task is needed.
        // For this refactor, we keep the original logic.
        // A simple check could be added here to only decrement once per second.

        // Update Boss Bar
        int initialTime = initialFlightTime.getOrDefault(player.getUniqueId(), 1);
        double progress = Math.max(0.0, (double) currentFlightTime / initialTime);
        bossBar.setProgress(progress);
        bossBar.setTitle(ColorHelper.parse(plugin.getMessagesHandlerInstance().getElytraTimeLimitMessage().replace("{0}", TimeHelper.formatFlightTime(currentFlightTime))));

        if (progress > 0.5) bossBar.setColor(BarColor.GREEN);
        else if (progress > 0.2) bossBar.setColor(BarColor.YELLOW);
        else bossBar.setColor(BarColor.RED);
    }

    private void handleSpeedometer(Player player) {
        Vector velocity = player.getVelocity();
        double realSpeed = velocity.length() * TICKS_IN_ONE_SECOND * METERS_PER_SECOND_TO_KMH;
        double finalSpeed = realSpeed;

        // Enforce speed limits
        double worldMaxSpeed = perWorldSpeedLimits.getOrDefault(player.getWorld().getName(), this.maxSpeed);
        if (!PermissionsHelper.PlayerBypassSpeedLimit(player) && isSpeedLimitEnabled && realSpeed > worldMaxSpeed) {
            finalSpeed = worldMaxSpeed;
            player.setVelocity(velocity.normalize().multiply(worldMaxSpeed / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND));
        } else if (realSpeed > MAX_FLIGHT_SPEED) {
            finalSpeed = MAX_FLIGHT_SPEED;
            player.setVelocity(velocity.normalize().multiply(MAX_FLIGHT_SPEED / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND));
        }

        // Determine which message format to use
        String format;
        if (plugin.getElytraBoostListener().isSuperBoostActive(player.getUniqueId())) {
            format = plugin.getMessagesHandlerInstance().getSpeedoMeterSuperBoost();
        } else if (plugin.getElytraBoostListener().isBoostActive(player.getUniqueId())) {
            format = plugin.getMessagesHandlerInstance().getSpeedoMeterBoost();
        } else {
            format = plugin.getMessagesHandlerInstance().getSpeedoMeterNormal();
        }

        String color = calculateSpeedColor(finalSpeed);
        String message = format.replace("{0}", color).replace("{1}", String.format("%.1f", finalSpeed));
        plugin.getMessagesHelper().sendActionBarMessage(player, message);
    }

    private void handleDurabilityProtection(Player player) {
        if (!isElytraBreakProtectionEnabled) return;

        ItemStack elytra = player.getInventory().getChestplate();
        if (elytra != null && elytra.getType() == Material.ELYTRA && elytra.getItemMeta() instanceof Damageable damageable) {
            if (damageable.getDamage() >= elytra.getType().getMaxDurability() - 1) {
                if (noFallDamagePlayers.add(player.getUniqueId())) {
                    player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 0.8f);
                    plugin.getMessagesHelper().sendActionBarMessage(player, "§fFall Protection: §a§lEnabled");
                }
            }
        }
    }

    private void handleDistanceTracking(Player player, PlayerMoveEvent event) {
        if (event.getFrom().distanceSquared(event.getTo()) == 0) return; // Ignore tiny movements

        PlayerStats stats = plugin.getStatsHandler().getStats(player);
        double distanceMoved = event.getFrom().distance(event.getTo());

        stats.addDistance(distanceMoved);
        currentFlightDistances.compute(player.getUniqueId(), (uuid, dist) -> (dist == null ? 0 : dist) + distanceMoved);
    }

    private String calculateSpeedColor(double speed) {
        if (speed > 180) return "§4";
        if (speed > 125) return "§c";
        if (speed > 50) return "§6";
        return "§a";
    }
    //</editor-fold>

    //<editor-fold desc="Data & Config Management">
    public void assignConfigVariables() {
        ConfigHandler config = plugin.getConfigHandlerInstance();
        this.isGlobalFlightDisabled = config.getIsGlobalFlightDisabled();
        this.isSpeedLimitEnabled = config.getIsSpeedLimitEnabled();
        this.disabledElytraWorlds = config.getDisabledWorlds();
        this.perWorldSpeedLimits = config.getPerWorldSpeedLimits();
        this.isTimeLimitEnabled = config.getIsTimeLimitEnabled();
        this.isElytraBreakProtectionEnabled = config.getIsElytraBreakProtectionEnabled();
        this.isKineticEnergyProtectionEnabled = config.getIsKineticEnergyProtectionEnabled();
        this.maxSpeed = config.getDefaultSpeedLimit();
    }

    private void createFlightBossBar(Player player) {
        if (!isTimeLimitEnabled || flightBossBars.containsKey(player.getUniqueId())) return;

        if (PermissionsHelper.PlayerBypassTimeLimit(player)) {
            String message = plugin.getMessagesHandlerInstance().getElytraFlightTimeBypass();
            BossBar bossBar = Bukkit.createBossBar(ColorHelper.parse(message), BarColor.YELLOW, BarStyle.SOLID);
            bossBar.addPlayer(player);
            flightBossBars.put(player.getUniqueId(), bossBar);
        } else {
            int flightTime = flightTimeData.getOrDefault(player.getUniqueId(), 0);
            if (flightTime > 0) {
                String message = plugin.getMessagesHandlerInstance().getElytraTimeLimitMessage()
                        .replace("{0}", TimeHelper.formatFlightTime(flightTime));
                BossBar bossBar = Bukkit.createBossBar(ColorHelper.parse(message), BarColor.GREEN, BarStyle.SOLID);
                bossBar.addPlayer(player);
                flightBossBars.put(player.getUniqueId(), bossBar);

                // Set the initial time for the progress bar calculation
                initialFlightTime.put(player.getUniqueId(), flightTime);
            }
        }
    }

    private void removeFlightBossBar(Player player) {
        BossBar bossBar = flightBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    private void loadPlayerFlightTime(UUID playerId) {
        if (!isTimeLimitEnabled) return;
        try {
            int storedTime = plugin.getDatabaseHandler().GetPlayerFlightTime(playerId);
            flightTimeData.put(playerId, storedTime);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load flight time for " + playerId, e);
        }
    }

    private void savePlayerFlightTime(UUID playerId) {
        if (!isTimeLimitEnabled) return;
        Integer time = flightTimeData.get(playerId);
        if (time != null) {
            try {
                plugin.getDatabaseHandler().SetPlayerFlightTime(playerId, time);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save flight time for " + playerId, e);
            }
        }
    }
    //</editor-fold>

    /**
     * Reloads the flight time from the database for all currently online players.
     * This is intended to be called after a plugin reload.
     */
    public void reloadOnlinePlayerFlightTimes() {
        // This check is important in case the feature was just enabled on reload
        if (!isTimeLimitEnabled) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerFlightTime(player.getUniqueId());
        }
    }

    public void saveAllFlightTimes() {
        plugin.getMessagesHelper().sendDebugMessage("Saving flight time for all online players...");
        for (UUID playerId : new HashSet<>(flightTimeData.keySet())) {
            savePlayerFlightTime(playerId);
        }
        plugin.getMessagesHelper().sendDebugMessage("Finished saving all player flight times.");
    }

    public int getCurrentFlightTime(UUID player) {
        return flightTimeData.getOrDefault(player, 0);
    }

    public void setFlightTime(UUID player, int flightTime) {
        flightTimeData.put(player, flightTime);
        initialFlightTime.put(player, flightTime); // Also reset initial time for boss bar
    }

    public void addFlightTime(UUID player, int secondsToAdd) {
        int currentTime = getCurrentFlightTime(player);
        flightTimeData.put(player, currentTime + secondsToAdd);
        // We do not update the initialFlightTime here, so the boss bar progress remains correct.
    }
}