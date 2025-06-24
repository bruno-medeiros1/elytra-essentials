package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

public class ElytraFlightListener implements Listener
{
    private static final int TICKS_IN_ONE_SECOND = 20;
    private static final double METERS_PER_SECOND_TO_KMH = 3.6; // Conversion factor: 1 m/s = 3.6 km/h
    private static final double SPEED_SLOW_THRESHOLD = 50.0;
    private static final double SPEED_NORMAL_THRESHOLD = 125.0;
    private static final double SPEED_FAST_THRESHOLD = 180;

    private static final double MAX_FLIGHT_SPEED = 200;
    private static final double MAX_SPEED_BLOCKS_PER_TICK = MAX_FLIGHT_SPEED / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND;

    private final String timeExpiredMessage;
    private final String timeLimitMessageTemplate;

    private final ElytraEssentials plugin;

    //  config values
    private int maxFlightTime;

    private double maxSpeed;
    private double maxSpeedBlocksPerTick;

    private boolean isGlobalFlightDisabled;
    private boolean isSpeedLimitEnabled;
    private boolean isTimeLimitEnabled;
    private boolean isElytraBreakProtectionEnabled;
    private boolean isKineticEnergyProtectionEnabled;

    private List disabledElytraWorlds;
    private HashMap<String, Double> perWorldSpeedLimits;

    private final HashMap<UUID, Integer> initialFlightTimeLeft;
    private final HashMap<UUID, Integer> flightTimeLeft;
    private final Map<UUID, Long> bossBarUpdateTimes = new HashMap<>();
    private final HashMap<UUID, Double> currentFlightDistances = new HashMap<>();

    private final HashMap<UUID, BossBar> flightBossBars;
    private final HashSet<Player> noFallDamagePlayers;

    private ElytraEffect playerActiveEffect = null;

    public ElytraFlightListener(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.perWorldSpeedLimits = new HashMap<>();

        this.initialFlightTimeLeft = new HashMap<>();
        this.flightTimeLeft = new HashMap<>();
        this.flightBossBars = new HashMap<>();
        this.noFallDamagePlayers = new HashSet<>();

        AssignConfigVariables();

        this.timeExpiredMessage = ChatColor.translateAlternateColorCodes('&',
                this.plugin.getMessagesHandlerInstance().getElytraFlightTimeExpired());
        this.timeLimitMessageTemplate = ChatColor.translateAlternateColorCodes('&',
                this.plugin.getMessagesHandlerInstance().getElytraTimeLimitMessage());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) throws SQLException {
        plugin.getStatsHandler().loadPlayerStats(e.getPlayer());

        UUID playerId = e.getPlayer().getUniqueId();

        String effectName = plugin.getDatabaseHandler().getPlayerActiveEffect(playerId);
        if (effectName != null){
            var effects = this.plugin.getEffectsHandler().getEffectsRegistry();
            playerActiveEffect = effects.getOrDefault(effectName, null);
        }

        if (!isTimeLimitEnabled)
            return;

        HandleFlightTime(playerId, false);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) throws SQLException {
        plugin.getStatsHandler().savePlayerStats(e.getPlayer());

        UUID playerId = e.getPlayer().getUniqueId();

        // Clean up maps to prevent memory leaks
        flightBossBars.remove(playerId);
        currentFlightDistances.remove(playerId);
        noFallDamagePlayers.remove(e.getPlayer());
        bossBarUpdateTimes.remove(playerId);
        initialFlightTimeLeft.remove(playerId);

        if (!isTimeLimitEnabled)
            return;

        HandleFlightTime(playerId, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        plugin.getStatsHandler().setGliding(player, e.isGliding()); // Update gliding state for time tracking

        String playerWorld = player.getWorld().getName();

        if (this.isGlobalFlightDisabled) {
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getElytraUsageDisabledMessage());
            e.setCancelled(true);
            return;
        }

        if (this.disabledElytraWorlds != null && this.disabledElytraWorlds.contains(playerWorld)) {
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getElytraUsageWorldDisabledMessage());
            e.setCancelled(true);
            return;
        }

        //  player is contained in a speed limited world
        if (this.isSpeedLimitEnabled && this.perWorldSpeedLimits != null && this.perWorldSpeedLimits.containsKey(playerWorld)) {
            this.maxSpeed = perWorldSpeedLimits.get(playerWorld);
            this.maxSpeedBlocksPerTick = this.maxSpeed / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND;
        }

        //  Flight Time Handling
        UUID playerId = player.getUniqueId();
        if (isTimeLimitEnabled){

            if (PermissionsHelper.PlayerBypassTimeLimit(player)) {
                //  TODO: Add customizable boss bar colors
                if (!flightBossBars.containsKey(playerId)){
                    String message = plugin.getMessagesHandlerInstance().getElytraBypassTimeLimitMessage();
                    message = ChatColor.translateAlternateColorCodes('&', message);
                    BossBar bossBar = Bukkit.createBossBar(message, BarColor.YELLOW, BarStyle.SOLID);
                    bossBar.addPlayer(player);

                    flightBossBars.put(playerId, bossBar);
                }
            }
            else {
                int flightTime = flightTimeLeft.getOrDefault(playerId, 0);
                if (flightTime > 0 ){
                    if (!flightBossBars.containsKey(playerId)) {

                        String timeLimitMessage = timeLimitMessageTemplate.replace("{0}", TimeHelper.formatFlightTime(flightTime));
                        BossBar bossBar = Bukkit.createBossBar(timeLimitMessage, BarColor.GREEN, BarStyle.SOLID);
                        bossBar.addPlayer(player);

                        flightBossBars.put(playerId, bossBar);
                        bossBarUpdateTimes.put(playerId, System.currentTimeMillis());

                        initialFlightTimeLeft.put(playerId, flightTime);
                    }
                }
            }
        }

        //  Stats Tracker
        if (e.isGliding()) {
            currentFlightDistances.put(player.getUniqueId(), 0.0);
        } else {
            PlayerStats stats = plugin.getStatsHandler().getStats(player);
            double flightDistance = currentFlightDistances.getOrDefault(player.getUniqueId(), 0.0);

            if (flightDistance > stats.getLongestFlight()) {
                stats.setLongestFlight(flightDistance);
            }
            currentFlightDistances.remove(player.getUniqueId());
        }
    }

    //  TODO: Add methods to handle each logic in a more sustainable way
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!player.isGliding()){
            BossBar bossBar = flightBossBars.remove(playerId);
            if (bossBar != null)
                bossBar.removeAll();

            bossBarUpdateTimes.remove(playerId);
            return;
        }

        //  Flight Time Handling
        if (this.isTimeLimitEnabled && !PermissionsHelper.PlayerBypassTimeLimit(player)) {
            int flightTime = flightTimeLeft.getOrDefault(playerId, 0);
            if (flightTime <= 0) {
                // Stop flight when time runs out
                player.setGliding(false);

                // Elytra deactivated, add player to the no-fall-damage list
                noFallDamagePlayers.add(player);

                flightTimeLeft.remove(playerId);

                BossBar bossBar = flightBossBars.remove(playerId);
                if (bossBar != null)
                    bossBar.removeAll();

                bossBarUpdateTimes.remove(playerId);

                plugin.getMessagesHelper().sendActionBarMessage(player, timeExpiredMessage);
                return;
            }

            // Debounce check for boss bar updates
            long currentTime = System.currentTimeMillis();
            long lastUpdateTime = bossBarUpdateTimes.getOrDefault(playerId, 0L);

            if (currentTime - lastUpdateTime >= 1000) { // 1 second debounce interval
                // Update flight time
                //Bukkit.getLogger().info("Updating flight time for player: " + player.getName() + ", Time Left: " + flightTime);

                flightTime--;
                flightTimeLeft.put(playerId, flightTime);

                BossBar bossBar = flightBossBars.get(playerId);
                if (bossBar != null) {
                    int initialTime = initialFlightTimeLeft.getOrDefault(playerId, 1); // Avoid division by zero
                    //Bukkit.getLogger().info("Initial Time: " + initialTime + "s | Flight time Left: " + flightTime + "s");

                    float progress = Math.max(0f, Math.min(1f, (float) flightTime / initialTime));
                    bossBar.setProgress(progress);

                    // Bar colors indicating flight time is finishing
                    if (progress > 0.5f)
                        bossBar.setColor(BarColor.GREEN);
                    else if (progress <= 0.5f && progress >= 0.2)
                        bossBar.setColor(BarColor.YELLOW);
                    else {
                        bossBar.setColor(BarColor.RED);
                    }

                    String timeLimitMessage = timeLimitMessageTemplate.replace("{0}", TimeHelper.formatFlightTime(flightTime));
                    bossBar.setTitle(timeLimitMessage);
                }

                // Update the last update time
                bossBarUpdateTimes.put(playerId, currentTime);
            }
        }

        if (this.isElytraBreakProtectionEnabled) {
            ItemStack elytra = player.getInventory().getChestplate();

            // Check if they are wearing an elytra and it can be damaged
            if (elytra != null && elytra.getType() == Material.ELYTRA && elytra.getItemMeta() instanceof Damageable damageable) {
                int currentDamage = damageable.getDamage();
                int maxDurability = elytra.getType().getMaxDurability();

                //  just before it breaks
                if (currentDamage >= maxDurability - 1) {
                    if (noFallDamagePlayers.add(player)) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 0.8f, 0.8f);
                        plugin.getMessagesHelper().sendActionBarMessage(player, "§fFall Protection: §a§lEnabled");
                        return;
                    }
                }
            }
        }

        //  Calculate the player's current, real speed
        Vector velocity = player.getVelocity();
        double realSpeed = velocity.length() * TICKS_IN_ONE_SECOND * METERS_PER_SECOND_TO_KMH;

        //  Determine the final speed to apply and display, enforcing limits
        double finalSpeed = realSpeed;
        boolean playerBypassSpeedLimit = PermissionsHelper.PlayerBypassSpeedLimit(player);

        if (!playerBypassSpeedLimit && this.isSpeedLimitEnabled && realSpeed > this.maxSpeed) {
            // Player is over the world/permission-based speed limit
            finalSpeed = this.maxSpeed;
            player.setVelocity(velocity.normalize().multiply(this.maxSpeedBlocksPerTick));
        } else if (realSpeed > MAX_FLIGHT_SPEED) {
            // Player is over the hard-coded max speed
            finalSpeed = MAX_FLIGHT_SPEED;
            player.setVelocity(velocity.normalize().multiply(MAX_SPEED_BLOCKS_PER_TICK));
        }

        Long boostExpiryTime = plugin.getElytraBoostListener().getBoostMessageExpirations().get(player.getUniqueId());
        Long superBoostExpiryTime = plugin.getElytraBoostListener().getSuperBoostMessageExpirations().get(player.getUniqueId());

        boolean showBoostMessage = boostExpiryTime != null && System.currentTimeMillis() < boostExpiryTime;
        boolean showSuperBoostMessage = superBoostExpiryTime != null && System.currentTimeMillis() < superBoostExpiryTime;

        //  Choose the message to display, prioritizing the super boost
        String message;
        String color = CalculateSpeedColor(finalSpeed);

        if (showSuperBoostMessage) {
            message = String.format("§a§l++ §eSpeed: %s%.1f §ekm/h §a§l++", color, finalSpeed);
        } else if (showBoostMessage) {
            message = String.format("§a§l+ §eSpeed: %s%.1f §ekm/h §a§l+", color, finalSpeed);
        } else {
            message = String.format("§eSpeed: %s%.1f §ekm/h", color, finalSpeed);

            // Clean up any expired timers from the maps
            if (boostExpiryTime != null) {
                plugin.getElytraBoostListener().getBoostMessageExpirations().remove(player.getUniqueId());
            }
            if (superBoostExpiryTime != null) {
                plugin.getElytraBoostListener().getSuperBoostMessageExpirations().remove(player.getUniqueId());
            }
        }

        //  Build and send the final action bar message ONCE
        plugin.getMessagesHelper().sendActionBarMessage(player, message);

        //  Spawn Player's Elytra Effect
        if (playerActiveEffect != null && !plugin.getTpsHandler().isLagProtectionActive()) {
            plugin.getEffectsHandler().spawnParticleTrail(player, playerActiveEffect);
        }

        //  Track Stats
        PlayerStats stats = plugin.getStatsHandler().getStats(player);
        double distanceMoved;
        if (event.getTo() != null)
            distanceMoved = event.getFrom().distance(event.getTo());
        else
            distanceMoved = 0;

        // Add to total distance
        stats.addDistance(distanceMoved);

        // Add to current flight distance (for longest flight calculation)
        currentFlightDistances.compute(player.getUniqueId(), (uuid, dist) -> (dist == null ? 0 : dist) + distanceMoved);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        PlayerStats stats = plugin.getStatsHandler().getStats(player);

        // First, check for high-speed wall collisions.
        if (this.isKineticEnergyProtectionEnabled && e.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            e.setCancelled(true);
            stats.incrementPluginSaves();
            return;
        }

        // Check if the damage is fall damage and the player is in the no-fall-damage list
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && noFallDamagePlayers.contains(player)) {
            e.setCancelled(true);
            stats.incrementPluginSaves();
            noFallDamagePlayers.remove(player);
        }
    }

    private String CalculateSpeedColor(double speed) {
        if (speed > 0 && speed <= SPEED_SLOW_THRESHOLD)
            return "§a";
        if (speed > SPEED_SLOW_THRESHOLD && speed <= SPEED_NORMAL_THRESHOLD)
            return "§6";
        if (speed > SPEED_NORMAL_THRESHOLD && speed <= SPEED_FAST_THRESHOLD)
            return "§c";
        if (speed > SPEED_FAST_THRESHOLD)
            return "§4";
        else
            return "§7";
    }

    public void AssignConfigVariables() {
        ConfigHandler configHandler = plugin.getConfigHandlerInstance();

        this.maxSpeed = configHandler.getDefaultSpeedLimit();
        this.maxSpeedBlocksPerTick = this.maxSpeed / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND;

        this.isGlobalFlightDisabled = configHandler.getIsGlobalFlightDisabled();
        this.isSpeedLimitEnabled = configHandler.getIsSpeedLimitEnabled();
        this.disabledElytraWorlds = configHandler.getDisabledWorlds();
        this.perWorldSpeedLimits = configHandler.getPerWorldSpeedLimits();
        this.isTimeLimitEnabled = configHandler.getIsTimeLimitEnabled();
        this.maxFlightTime = configHandler.getMaxTimeLimit();
        this.isElytraBreakProtectionEnabled = configHandler.getIsElytraBreakProtectionEnabled();
        this.isKineticEnergyProtectionEnabled = configHandler.getIsKineticEnergyProtectionEnabled();
    }

    /// Method used on plugin reload to Handle time flight
    public void validateFlightTimeOnReload() throws SQLException {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PermissionsHelper.PlayerBypassTimeLimit(player))
                continue;

            UUID playerId = player.getUniqueId();
            HandleFlightTime(playerId, false);
        }
    }

    public Map<UUID, Integer> GetAllActiveFlights() { return flightTimeLeft; }

    public void UpdatePlayerFlightTime(UUID player, int flightTime) {
        initialFlightTimeLeft.put(player, flightTime);
        flightTimeLeft.put(player, flightTime);
    }

    private void HandleFlightTime(UUID playerId, boolean onPlayerQuitEvent) throws SQLException {
        if (onPlayerQuitEvent){
            int newTime = flightTimeLeft.getOrDefault(playerId, 0);
            if (newTime > maxFlightTime)
                newTime = maxFlightTime;

            plugin.getDatabaseHandler().SetPlayerFlightTime(playerId, newTime);

            initialFlightTimeLeft.remove(playerId);
            flightTimeLeft.remove(playerId);
            return;
        }

        int storedTime = plugin.getDatabaseHandler().GetPlayerFlightTime(playerId);
        if (maxFlightTime > 0 && storedTime > maxFlightTime)
            storedTime = maxFlightTime;

        UpdatePlayerFlightTime(playerId, storedTime);
    }

    public void UpdateEffect(ElytraEffect effect){
        this.playerActiveEffect = effect;
    }
}