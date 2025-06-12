package org.bruno.elytraEssentials.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.*;

/// Vanilla Speed Elytra:
///
/// Without rockets: 36 km/h
/// With rockets (normal flight): 108 km/h
/// With rockets (steep dive): 180 km/h
///
public class ElytraFlightListener implements Listener
{
    private static final int TICKS_IN_ONE_SECOND = 20;
    private static final double METERS_PER_SECOND_TO_KMH = 3.6; // Conversion factor: 1 m/s = 3.6 km/h
    private static final double SPEED_SLOW_THRESHOLD = 50.0;
    private static final double SPEED_NORMAL_THRESHOLD = 125.0;
    private static final double SPEED_FAST_THRESHOLD = 180;

    private final String timeExpiredMessage;
    private final String timeLimitMessageTemplate;


    private final ElytraEssentials elytraEssentials;

    //  config values
    private double maxSpeed;
    private double maxSpeedBlocksPerTick;

    private boolean isGlobalFlightDisabled;
    private boolean isSpeedLimitEnabled;
    private boolean isTimeLimitEnabled;

    private List disabledElytraWorlds;
    private HashMap<String, Double> perWorldSpeedLimits;

    private final HashMap<UUID, Integer> initialFlightTimeLeft;
    private final HashMap<UUID, Integer> flightTimeLeft;
    private final Map<UUID, Long> bossBarUpdateTimes = new HashMap<>();

    private final HashMap<UUID, BossBar> flightBossBars;
    private final HashSet<Player> noFallDamagePlayers;

    public ElytraFlightListener(ElytraEssentials elytraEssentials) {
        this.elytraEssentials = elytraEssentials;
        this.perWorldSpeedLimits = new HashMap<>();

        this.initialFlightTimeLeft = new HashMap<>();
        this.flightTimeLeft = new HashMap<>();
        this.flightBossBars = new HashMap<>();
        this.noFallDamagePlayers = new HashSet<>();

        AssignConfigVariables();

        this.timeExpiredMessage = ChatColor.translateAlternateColorCodes('&',
                this.elytraEssentials.getMessagesHandlerInstance().getElytraFlightTimeExpired());
        this.timeLimitMessageTemplate = ChatColor.translateAlternateColorCodes('&',
                this.elytraEssentials.getMessagesHandlerInstance().getElytraTimeLimitMessage());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!isTimeLimitEnabled)
            return;

        UUID uuid = e.getPlayer().getUniqueId();

        try {
            int storedTime = this.elytraEssentials.getDatabaseHandler().GetPlayerFlightTime(uuid);
            Bukkit.getLogger().info("Player " + e.getPlayer().getName() + " joined! Flight time: " + storedTime + "s");

            initialFlightTimeLeft.put(uuid, storedTime);
            flightTimeLeft.put(uuid, storedTime);

            Bukkit.getLogger().info("flightTimeLeft: " + this.flightTimeLeft);
        } catch (SQLException ex) {
            this.elytraEssentials.getMessagesHelper().SendDebugMessage("Something went wrong while trying to get " + e.getPlayer().getName() + " flight time");
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (!isTimeLimitEnabled)
            return;

        UUID uuid = e.getPlayer().getUniqueId();

        try {
            int newTime = flightTimeLeft.getOrDefault(uuid, 0);
            Bukkit.getLogger().info("Set Player " + e.getPlayer().getName() + " with UUID " + uuid + " flight time to: " + newTime);
            this.elytraEssentials.getDatabaseHandler().SetPlayerFlightTime(uuid, newTime);

            initialFlightTimeLeft.remove(uuid);
            flightTimeLeft.remove(uuid);

        } catch (SQLException ex) {
            this.elytraEssentials.getMessagesHelper().SendDebugMessage("Something went wrong while trying to set " + e.getPlayer().getName() + " flight time");
            throw new RuntimeException(ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerGlide(EntityToggleGlideEvent e) {
        Player player = (Player) e.getEntity();
        String playerWorld = player.getWorld().getName();

        if (this.isGlobalFlightDisabled) {
            this.elytraEssentials.getMessagesHelper().sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraUsageDisabledMessage());
            e.setCancelled(true);
            return;
        }

        if (this.disabledElytraWorlds != null && this.disabledElytraWorlds.contains(playerWorld)) {
            this.elytraEssentials.getMessagesHelper().sendPlayerMessage(player, elytraEssentials.getMessagesHandlerInstance().getElytraUsageWorldDisabledMessage());
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

            if (PlayerBypassTimeLimit(player)) {
                //  TODO: Add customizable boss bar colors
                if (!flightBossBars.containsKey(playerId)){
                    String message = this.elytraEssentials.getMessagesHandlerInstance().getElytraBypassTimeLimitMessage();
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

                        String timeLimitMessage = timeLimitMessageTemplate.replace("{0}", String.valueOf(flightTime));
                        BossBar bossBar = Bukkit.createBossBar(timeLimitMessage, BarColor.GREEN, BarStyle.SOLID);
                        bossBar.addPlayer(player);

                        flightBossBars.put(playerId, bossBar);
                        bossBarUpdateTimes.put(playerId, System.currentTimeMillis());

                        initialFlightTimeLeft.put(playerId, flightTime);
                    }
                }
            }
        }
    }

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
        if (this.isTimeLimitEnabled && !PlayerBypassTimeLimit(player)) {
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

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(timeExpiredMessage));
                return;
            }

            // Debounce check for boss bar updates
            long currentTime = System.currentTimeMillis();
            long lastUpdateTime = bossBarUpdateTimes.getOrDefault(playerId, 0L);

            if (currentTime - lastUpdateTime >= 1000) { // 1 second debounce interval
                // Update flight time
                Bukkit.getLogger().info("Updating flight time for player: " + player.getName() + ", Time Left: " + flightTime);

                flightTime--;
                flightTimeLeft.put(playerId, flightTime);

                BossBar bossBar = flightBossBars.get(playerId);
                if (bossBar != null) {
                    int initialTime = initialFlightTimeLeft.getOrDefault(playerId, 1); // Avoid division by zero
                    Bukkit.getLogger().info("Initial Time: " + initialTime + "s | Flight time Left: " + flightTime + "s");

                    float progress = Math.max(0f, Math.min(1f, (float) flightTime / initialTime));
                    bossBar.setProgress(progress);

                    //TODO: Review Bar colors indicating flight time is finishing
                    if (progress > 0.5f)
                        bossBar.setColor(BarColor.GREEN);
                    else if (progress <= 0.5f && progress >= 0.2)
                        bossBar.setColor(BarColor.YELLOW);
                    else {
                        bossBar.setColor(BarColor.RED);
                    }

                    String timeLimitMessage = timeLimitMessageTemplate.replace("{0}", String.valueOf(flightTime));
                    bossBar.setTitle(timeLimitMessage);
                }

                // Update the last update time
                bossBarUpdateTimes.put(playerId, currentTime);
            }
        }

        // Calculate speed (convert velocity magnitude to km/h)
        Vector velocity = player.getVelocity();
        double speed = velocity.length() * TICKS_IN_ONE_SECOND  * METERS_PER_SECOND_TO_KMH;

        String color = CalculateSpeedColor(speed);

        boolean playerBypassSpeedLimit = PlayerBypassSpeedLimit(player);

        if (!playerBypassSpeedLimit && this.isSpeedLimitEnabled && speed > this.maxSpeed)
        {
            color = CalculateSpeedColor(this.maxSpeed);

            // Snap velocity to max speed
            Vector snappedVelocity = velocity.normalize().multiply(this.maxSpeedBlocksPerTick);
            player.setVelocity(snappedVelocity);

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacy("§eSpeed: " + color + String.format("%.2f", this.maxSpeed) + " §ekm/h"));
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacy("§eSpeed: " + color + String.format("%.2f", speed) + " §ekm/h"));
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        // Check if the damage is fall damage and the player is in the no-fall-damage list
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && noFallDamagePlayers.contains(player)) {
            e.setCancelled(true);
            noFallDamagePlayers.remove(player); // Remove after preventing damage to avoid permanent immunity
        }
    }

    private boolean PlayerBypassSpeedLimit(Player player) {
        return player.hasPermission("elytraessentials.bypass.speedlimit") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                player.hasPermission("elytraessentials.*");
    }

    private boolean PlayerBypassTimeLimit(Player player) {
        return player.hasPermission("elytraessentials.bypass.timelimit") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                player.hasPermission("elytraessentials.*");
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
        ConfigHandler configHandler = elytraEssentials.getConfigHandlerInstance();

        this.maxSpeed = configHandler.getDefaultSpeedLimit();
        this.maxSpeedBlocksPerTick = this.maxSpeed / METERS_PER_SECOND_TO_KMH / TICKS_IN_ONE_SECOND;

        this.isGlobalFlightDisabled = configHandler.getIsGlobalFlightDisabled();
        this.isSpeedLimitEnabled = configHandler.getIsSpeedLimitEnabled();
        this.disabledElytraWorlds = configHandler.getDisabledWorlds();
        this.perWorldSpeedLimits = configHandler.getPerWorldSpeedLimits();
        this.isTimeLimitEnabled = configHandler.getIsTimeLimitEnabled();
    }

    public Map<UUID, Integer> GetAllActiveFlights() { return flightTimeLeft; }

    public void UpdatePlayerFlightTime(UUID player, int flightTime){
        initialFlightTimeLeft.put(player, flightTime);
        flightTimeLeft.put(player, flightTime);
    }
}