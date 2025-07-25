package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.helpers.*;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlightHandler {
    private static final int TICKS_IN_ONE_SECOND = 20;
    private static final double METERS_PER_SECOND_TO_KMH = 3.6;
    private static final double MAX_FLIGHT_SPEED = 200.0;
    private static final long DEPLOY_COOLDOWN_MS = 5000;
    private static final int DEPLOY_MIN_FALL_DISTANCE = 5;

    private final Logger logger;
    private final ConfigHandler configHandler;
    private final EffectsHandler effectsHandler;
    private final BoostHandler boostHandler;
    private final FoliaHelper foliaHelper;
    private final MessagesHelper messagesHelper;
    private final DatabaseHandler databaseHandler;
    private final StatsHandler statsHandler;
    private final MessagesHandler messagesHandler;

    private final Map<UUID, Integer> flightTimeData = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> flightBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Double> currentFlightDistances = new ConcurrentHashMap<>();
    private final Set<UUID> noFallDamagePlayers = ConcurrentHashMap.newKeySet(); // Thread-safe set
    private final Map<UUID, Integer> initialFlightTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> deployCooldowns = new ConcurrentHashMap<>();

    private CancellableTask globalFlightTask;
    private final Map<UUID, CancellableTask> activeFlightTasks = new HashMap<>();

    public FlightHandler(Logger logger, ConfigHandler configHandler, EffectsHandler effectsHandler, BoostHandler boostHandler, FoliaHelper foliaHelper,
                         MessagesHelper messagesHelper, DatabaseHandler databaseHandler, StatsHandler statsHandler, MessagesHandler messagesHandler) {
        this.logger = logger;
        this.configHandler = configHandler;
        this.effectsHandler = effectsHandler;
        this.boostHandler = boostHandler;
        this.foliaHelper = foliaHelper;
        this.messagesHelper = messagesHelper;
        this.databaseHandler = databaseHandler;
        this.statsHandler = statsHandler;
        this.messagesHandler = messagesHandler;
    }

    public void start() {
        if (!configHandler.getIsTimeLimitEnabled()) return;

        // Since Folia is event-driven, we only need to start the global task if we're not on Folia
        if (!foliaHelper.isFolia()) {
            if (globalFlightTask != null) return;

            this.globalFlightTask = foliaHelper.runTaskTimerGlobal(this::handleGlobalFlightTimeCountdown, 20L, 20L);
        }
    }

    public void shutdown() {
        if (foliaHelper.isFolia()) {
            activeFlightTasks.values().forEach(CancellableTask::cancel);
            activeFlightTasks.clear();
        } else {
            if (globalFlightTask != null) {
                globalFlightTask.cancel();
            }
        }

        // Now, perform the common cleanup for both platforms
        flightBossBars.values().forEach(bossBar -> bossBar.setVisible(false));
        flightBossBars.clear();
        saveAllFlightTimes();
    }

    public void loadPlayerData(Player player) {
        if (!configHandler.getIsTimeLimitEnabled()) return;
        try {
            int storedTime = databaseHandler.getPlayerFlightTime(player.getUniqueId());
            flightTimeData.put(player.getUniqueId(), storedTime);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load flight time for " + player.getName(), e);
        }
    }

    public void unloadPlayerData(Player player) {
        savePlayerFlightTime(player.getUniqueId());

        // Clean up all player data to prevent memory leaks
        flightTimeData.remove(player.getUniqueId());
        flightBossBars.remove(player.getUniqueId());
        currentFlightDistances.remove(player.getUniqueId());
        noFallDamagePlayers.remove(player.getUniqueId());
        initialFlightTime.remove(player.getUniqueId());
        deployCooldowns.remove(player.getUniqueId());

        if (foliaHelper.isFolia()) {
            stopTrackingPlayer(player);
        }
    }

    public void saveAllFlightTimes() {
        for (UUID playerId : new HashSet<>(flightTimeData.keySet())) {
            savePlayerFlightTime(playerId);
        }
    }

    public boolean isProtectedFromFallDamage(UUID playerId) { return noFallDamagePlayers.contains(playerId); }
    public void removeFallProtection(UUID playerId) { noFallDamagePlayers.remove(playerId); }
    public boolean isKineticProtectionEnabled() { return configHandler.getIsKineticEnergyProtectionEnabled(); }

    public int getCurrentFlightTime(UUID playerId) {
        return flightTimeData.getOrDefault(playerId, 0);
    }

    /**
     * Checks if a player is allowed to start gliding.
     * @param player The player attempting to glide.
     * @return {@code true} if the glide event should be cancelled, {@code false} otherwise.
     */
    public boolean onGlideStartAttempt(Player player) {
        if (configHandler.getIsTimeLimitEnabled() && !PermissionsHelper.playerBypassTimeLimit(player)) {
            int currentFlightTime = flightTimeData.getOrDefault(player.getUniqueId(), 0);
            if (currentFlightTime <= 0) {
                messagesHelper.sendActionBarMessage(player, messagesHandler.getElytraFlightTimeExpired());
                return true;
            }
        }

        // Check for world/global restrictions
        if (configHandler.getIsGlobalFlightDisabled() || (configHandler.getDisabledWorlds() != null && configHandler.getDisabledWorlds().contains(player.getWorld().getName()))) {
            String msg = configHandler.getIsGlobalFlightDisabled() ? messagesHandler.getElytraUsageDisabledMessage() : messagesHandler.getElytraUsageWorldDisabledMessage();
            messagesHelper.sendPlayerMessage(player, msg);
            return true;
        }

        // If all checks pass, proceed with normal glide setup
        currentFlightDistances.put(player.getUniqueId(), 0.0);
        if (configHandler.getIsTimeLimitEnabled()) {
            createBossBar(player);
            if (foliaHelper.isFolia()) {
                startTrackingPlayer(player);
            }
        }

        return false;
    }

    public void handleGlideEnd(Player player) {
        PlayerStats stats = statsHandler.getStats(player);
        double flightDistance = currentFlightDistances.getOrDefault(player.getUniqueId(), 0.0);

        if (flightDistance > stats.getLongestFlight()) {
            stats.setLongestFlight(flightDistance);
            String message = messagesHandler.getNewPRLongestFlightMessage().replace("{0}", String.format("%.0f", flightDistance));
            messagesHelper.sendPlayerMessage(player, message);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }

        currentFlightDistances.remove(player.getUniqueId());
        removeBossBar(player);

        if (foliaHelper.isFolia())
            stopTrackingPlayer(player);
    }

    public void handlePlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        handleEmergencyDeploy(player);

        if (!player.isGliding()) {
            if (flightBossBars.containsKey(player.getUniqueId())) removeBossBar(player);
            return;
        }

        handleSpeedometer(player);
        handleDurabilityProtection(player);
        handleDistanceTracking(player, event);

        effectsHandler.spawnParticleTrail(player);
    }

    public void addFlightTime(UUID playerId, int secondsToAdd, CommandSender feedbackRecipient) {
        try {
            int currentFlightTime;
            Player target = Bukkit.getPlayer(playerId);

            if (target != null && target.isOnline()) {
                currentFlightTime = this.flightTimeData.getOrDefault(playerId, 0);
            } else {
                currentFlightTime = databaseHandler.getPlayerFlightTime(playerId);
            }

            int maxTimeLimit = configHandler.getMaxTimeLimit();

            // These variables can be modified
            int amountToAdd = secondsToAdd;
            int sumFlightTime = currentFlightTime + secondsToAdd;

            if (maxTimeLimit > 0 && sumFlightTime > maxTimeLimit) {
                amountToAdd = maxTimeLimit - currentFlightTime;
                sumFlightTime = maxTimeLimit;
            }

            int finalAmount = amountToAdd;
            int newFlightTime = sumFlightTime;

            if (finalAmount <= 0) {
                foliaHelper.runTaskOnMainThread(() -> messagesHelper.sendCommandSenderMessage(feedbackRecipient, "&cPlayer already has the maximum flight time."));
                return;
            }

            databaseHandler.setPlayerFlightTime(playerId, newFlightTime);

            // Update the live cache and notify the player if they are online
            if (target != null && target.isOnline()) {
                foliaHelper.runTaskOnMainThread(() -> {
                    flightTimeData.put(playerId, newFlightTime);

                    // we add the time to this as well to keep the progress bar ratio correct.
                    initialFlightTime.computeIfPresent(playerId, (key, currentInitial) -> currentInitial + finalAmount);

                    String message = messagesHandler.getElytraFlightTimeAdded().replace("{0}", TimeHelper.formatFlightTime(finalAmount));
                    messagesHelper.sendPlayerMessage(target, message);
                });
            }
        } catch (SQLException e) {
            handleSqlException(feedbackRecipient, "add flight time", playerId, e);
        }
    }

    public void removeFlightTime(UUID playerId, int secondsToRemove, CommandSender sender) {
        try {
            int currentFlightTime = databaseHandler.getPlayerFlightTime(playerId);
            // Ensure flight time doesn't go below zero
            int newFlightTime = Math.max(0, currentFlightTime - secondsToRemove);
            int actualAmountRemoved = currentFlightTime - newFlightTime;

            databaseHandler.setPlayerFlightTime(playerId, newFlightTime);

            // Update the live cache and notify the player if they are online
            Player target = Bukkit.getPlayer(playerId);
            if (target != null && target.isOnline()) {
                foliaHelper.runTaskOnMainThread(() -> {
                    flightTimeData.put(playerId, newFlightTime);
                    String message = messagesHandler.getElytraFlightTimeRemoved().replace("{0}", TimeHelper.formatFlightTime(actualAmountRemoved));
                    messagesHelper.sendPlayerMessage(target, message);
                });
            }
            foliaHelper.runTaskOnMainThread(() ->
                    messagesHelper.sendCommandSenderMessage(sender, "&aRemoved " + TimeHelper.formatFlightTime(actualAmountRemoved) + " of flight time from " + Bukkit.getOfflinePlayer(playerId).getName() + "."));
        } catch (SQLException e) {
            handleSqlException(sender, "remove flight time", playerId, e);
        }
    }

    public void setFlightTime(UUID playerId, int secondsToSet, CommandSender sender) {
        try {
            int maxTimeLimit = configHandler.getMaxTimeLimit();
            // Cap the amount at the max limit if one is set
            int finalAmount = (maxTimeLimit > 0) ? Math.min(secondsToSet, maxTimeLimit) : secondsToSet;

            databaseHandler.setPlayerFlightTime(playerId, finalAmount);

            // Update the live cache and notify the player if they are online
            Player target = Bukkit.getPlayer(playerId);
            if (target != null && target.isOnline()) {
                foliaHelper.runTaskOnMainThread(() -> {
                    flightTimeData.put(playerId, finalAmount);
                    String message = messagesHandler.getElytraFlightTimeSet().replace("{0}", TimeHelper.formatFlightTime(finalAmount));
                    messagesHelper.sendPlayerMessage(target, message);
                });
            }
            foliaHelper.runTaskOnMainThread(() ->
                    messagesHelper.sendCommandSenderMessage(sender, "&aSet " + Bukkit.getOfflinePlayer(playerId).getName() + "'s flight time to " + TimeHelper.formatFlightTime(finalAmount)));
        } catch (SQLException e) {
            handleSqlException(sender, "set flight time", playerId, e);
        }
    }

    public void clearFlightTime(UUID playerId, CommandSender sender) {
        try {
            databaseHandler.setPlayerFlightTime(playerId, 0);

            OfflinePlayer target = Bukkit.getOfflinePlayer(playerId);

            // Update the live cache and notify the player if they are online.
            if (target.isOnline()) {
                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null) {
                    foliaHelper.runTaskOnMainThread(() -> {
                        flightTimeData.put(playerId, 0);
                        initialFlightTime.put(playerId, 0);
                        messagesHelper.sendPlayerMessage(onlineTarget, messagesHandler.getElytraFlightTimeCleared());
                    });
                }
            }

            // Send a confirmation message back to the command sender.
            foliaHelper.runTaskOnMainThread(() ->
                    messagesHelper.sendCommandSenderMessage(sender, "&aCleared all flight time for " + target.getName() + "."));

        } catch (SQLException e) {
            handleSqlException(sender, "clear flight time", playerId, e);
        }
    }

    private void handleSqlException(CommandSender sender, String action, UUID targetId, SQLException e) {
        logger.log(Level.SEVERE, "Failed to " + action + " for " + targetId, e);
        foliaHelper.runTaskOnMainThread(() ->
                messagesHelper.sendCommandSenderMessage(sender, "&cA database error occurred. Please check the console."));
    }

    // This is the global loop for Spigot/Paper
    private void handleGlobalFlightTimeCountdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isFlying = player.isGliding() && !PermissionsHelper.playerBypassTimeLimit(player);
            if (isFlying) {
                if (!flightBossBars.containsKey(player.getUniqueId())) createBossBar(player);
                updatePlayerFlight(player);
            } else {
                if (flightBossBars.containsKey(player.getUniqueId())) removeBossBar(player);
            }
        }
    }

    private void updatePlayerFlight(Player player) {
        if (!player.isOnline()) {
            if (foliaHelper.isFolia()) {
                stopTrackingPlayer(player);
            }
            removeBossBar(player);
            return;
        }

        int currentFlightTime = flightTimeData.getOrDefault(player.getUniqueId(), 0);
        if (currentFlightTime <= 0) {
            player.setGliding(false);
            noFallDamagePlayers.add(player.getUniqueId());
            foliaHelper.runTaskLater(player, () -> noFallDamagePlayers.remove(player.getUniqueId()), 40L);
            messagesHelper.sendActionBarMessage(player, messagesHandler.getElytraFlightTimeExpired());

            if (!foliaHelper.isFolia())
                removeBossBar(player);

            return;
        }

        flightTimeData.put(player.getUniqueId(), currentFlightTime - 1);
        updateBossBar(player, currentFlightTime);
    }

    private void startTrackingPlayer(Player player) {
        if (activeFlightTasks.containsKey(player.getUniqueId()) || PermissionsHelper.playerBypassTimeLimit(player))
            return;

        CancellableTask task = foliaHelper.runTaskTimerForEntity(player, () -> updatePlayerFlight(player), 0L, 20L);
        activeFlightTasks.put(player.getUniqueId(), task);
    }

    private void stopTrackingPlayer(Player player) {
        CancellableTask task = activeFlightTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private void handleSpeedometer(Player player) {
        Vector velocity = player.getVelocity();
        double realSpeed = velocity.length() * TICKS_IN_ONE_SECOND * METERS_PER_SECOND_TO_KMH;
        double finalSpeed = realSpeed;

        // Enforce speed limits
        double worldMaxSpeed = configHandler.getPerWorldSpeedLimits().getOrDefault(player.getWorld().getName(), configHandler.getDefaultSpeedLimit());
        if (!PermissionsHelper.playerBypassSpeedLimit(player) && configHandler.getIsSpeedLimitEnabled() && realSpeed > worldMaxSpeed) {
            finalSpeed = worldMaxSpeed;
            player.setVelocity(velocity.normalize().multiply(worldMaxSpeed / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND));
        } else if (realSpeed > MAX_FLIGHT_SPEED) {
            finalSpeed = MAX_FLIGHT_SPEED;
            player.setVelocity(velocity.normalize().multiply(MAX_FLIGHT_SPEED / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND));
        }

        // Determine which message format to use
        String format;
        if (boostHandler.isSuperBoostActive(player.getUniqueId())) {
            format = messagesHandler.getSpeedoMeterSuperBoost();
        } else if (boostHandler.isBoostActive(player.getUniqueId())) {
            format = messagesHandler.getSpeedoMeterBoost();
        } else {
            format = messagesHandler.getSpeedoMeterNormal();
        }

        String color = calculateSpeedColor(finalSpeed);
        String message = format.replace("{0}", color).replace("{1}", String.format("%.1f", finalSpeed));
        messagesHelper.sendActionBarMessage(player, message);
    }

    private void handleDurabilityProtection(Player player) {
        if (!configHandler.getIsElytraBreakProtectionEnabled()) return;

        ItemStack elytra = player.getInventory().getChestplate();
        if (elytra != null && elytra.getType() == Material.ELYTRA && elytra.getItemMeta() instanceof Damageable damageable) {
            if (damageable.getDamage() >= elytra.getType().getMaxDurability() - 1) {
                if (noFallDamagePlayers.add(player.getUniqueId())) {
                    player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 0.8f);
                    messagesHelper.sendActionBarMessage(player, messagesHandler.getFallProtectionEnabled());
                }
            }
        }
    }

    private void handleDistanceTracking(Player player, PlayerMoveEvent event) {
        if (event.getFrom().distanceSquared(event.getTo()) == 0) return; // Ignore tiny movements

        PlayerStats stats = statsHandler.getStats(player);
        double distanceMoved = event.getFrom().distance(event.getTo());

        stats.addDistance(distanceMoved);
        currentFlightDistances.compute(player.getUniqueId(), (uuid, dist) -> (dist == null ? 0 : dist) + distanceMoved);
    }

    private void handleEmergencyDeploy(Player player) {
        if (!configHandler.getIsEmergencyDeployEnabled() || player.isInsideVehicle() || player.getVelocity().getY() >= 0 || player.isGliding()) {
            return;
        }

        if (isOnEmergencyDeployCooldown(player.getUniqueId()) || player.getFallDistance() < DEPLOY_MIN_FALL_DISTANCE || !PermissionsHelper.hasAutoDeployPermission(player)) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        if (inventory.getChestplate() != null) return;

        for (int i = 0; i < Constants.Inventory.MAIN_INV_SIZE; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == Material.ELYTRA) {
                inventory.setChestplate(item.clone());
                item.setAmount(item.getAmount() - 1);

                player.setGliding(true);
                player.setFallDistance(0);

                Vector launchDirection = player.getLocation().getDirection().setY(0).normalize();
                Vector launchVelocity = launchDirection.multiply(0.6).setY(0.5);
                player.setVelocity(launchVelocity);

                deployCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + DEPLOY_COOLDOWN_MS);

                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 0.8f);
                messagesHelper.sendPlayerMessage(player, messagesHandler.getEmergencyDeploySuccess());
                break;
            }
        }
    }

    private boolean isOnEmergencyDeployCooldown(UUID playerId) {
        if (!deployCooldowns.containsKey(playerId)) return false;
        if (System.currentTimeMillis() > deployCooldowns.get(playerId)) {
            deployCooldowns.remove(playerId);
            return false;
        }
        return true;
    }

    private String calculateSpeedColor(double speed) {
        if (speed > 180) return "§4";
        if (speed > 125) return "§c";
        if (speed > 50) return "§6";
        return "§a";
    }

    private void savePlayerFlightTime(UUID playerId) {
        if (!configHandler.getIsTimeLimitEnabled()) return;
        Integer time = flightTimeData.get(playerId);
        if (time != null) {
            try {
                databaseHandler.setPlayerFlightTime(playerId, time);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save flight time for " + playerId, e);
            }
        }
    }

    private void createBossBar(Player player) {
        if (!configHandler.getIsTimeLimitEnabled() || flightBossBars.containsKey(player.getUniqueId())) return;

        if (PermissionsHelper.playerBypassTimeLimit(player)) {
            String message = messagesHandler.getElytraFlightTimeBypass();
            BossBar bossBar = Bukkit.createBossBar(ColorHelper.parse(message), BarColor.YELLOW, BarStyle.SOLID);
            bossBar.addPlayer(player);
            flightBossBars.put(player.getUniqueId(), bossBar);
        } else {
            int flightTime = flightTimeData.getOrDefault(player.getUniqueId(), 0);
            if (flightTime > 0) {
                String message = messagesHandler.getElytraTimeLimitMessage()
                        .replace("{0}", TimeHelper.formatFlightTime(flightTime));
                BossBar bossBar = Bukkit.createBossBar(ColorHelper.parse(message), BarColor.GREEN, BarStyle.SOLID);
                bossBar.addPlayer(player);
                flightBossBars.put(player.getUniqueId(), bossBar);

                // Set the initial time for the progress bar calculation
                initialFlightTime.put(player.getUniqueId(), flightTime);
            }
        }
    }

    private void updateBossBar(Player player, int currentFlightTime) {
        BossBar bossBar = flightBossBars.get(player.getUniqueId());
        if (bossBar == null) return;

        // Calculate progress based on the initial flight time when the bar was created
        int initialTime = initialFlightTime.getOrDefault(player.getUniqueId(), 1);
        double progress = Math.max(0.0, (double) currentFlightTime / initialTime);
        bossBar.setProgress(progress);

        // Update the title with the formatted time
        String title = messagesHandler.getElytraTimeLimitMessage().replace("{0}", TimeHelper.formatFlightTime(currentFlightTime));
        bossBar.setTitle(ColorHelper.parse(title));

        // Update the color based on the remaining time percentage
        if (progress > 0.5) {
            bossBar.setColor(BarColor.GREEN);
        } else if (progress > 0.2) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.RED);
        }
    }

    private void removeBossBar(Player player) {
        BossBar bossBar = flightBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
}