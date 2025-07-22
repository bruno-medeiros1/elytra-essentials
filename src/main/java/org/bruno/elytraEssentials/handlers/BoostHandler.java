package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BoostHandler {
    private static final long MINIMUM_SPAM_DELAY_MS = 50; // The absolute minimum cooldown
    private static final double BOOST_MULTIPLIER = 0.5;
    private static final double SUPER_BOOST_MULTIPLIER = 1.0;

    private final ElytraEssentials plugin;
    private final FoliaHelper foliaHelper;
    private final MessagesHelper messagesHelper;
    private final ServerVersion serverVersion;
    private final StatsHandler statsHandler;
    private final ConfigHandler configHandler;
    private final MessagesHandler messagesHandler;
    private FlightHandler flightHandler;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownMessageSent = new ConcurrentHashMap<>();
    private final Map<UUID, Long> superBoostMessageExpirations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> boostMessageExpirations = new ConcurrentHashMap<>();
    private final Map<UUID, CancellableTask> chargingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> chargeBossBars = new ConcurrentHashMap<>();

    public BoostHandler(ElytraEssentials plugin, FoliaHelper foliaHelper, MessagesHelper messagesHelper, ServerVersion serverVersion, StatsHandler statsHandler,
                        ConfigHandler configHandler, MessagesHandler messagesHandler) {
        this.plugin = plugin;

        this.foliaHelper = foliaHelper;
        this.messagesHelper = messagesHelper;
        this.serverVersion = serverVersion;
        this.statsHandler = statsHandler;
        this.configHandler = configHandler;
        this.messagesHandler = messagesHandler;
    }

    public void setFlightHandler(FlightHandler flightHandler) {
        this.flightHandler = flightHandler;
    }

    public void handleInteract(Player player, boolean isGliding, boolean isSneaking, boolean isOnGround) {
        if (isGliding) {
            handleInAirBoost(player);
        } else if (isSneaking && isOnGround) {
            handleChargedJump(player);
        }
    }

    public void handlePlayerQuit(PlayerQuitEvent event) {
        // Clean up all maps when a player leaves
        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.remove(playerId);
        cooldownMessageSent.remove(playerId);
        boostMessageExpirations.remove(playerId);
        superBoostMessageExpirations.remove(playerId);
        cancelCharge(event.getPlayer()); // Cancel any active charge
    }

    public void handleToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Cancel the charge if a player stops sneaking
        if (!event.isSneaking() && chargingTasks.containsKey(player.getUniqueId())) {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
            cancelCharge(player);
        }
    }

    /**
     * Checks if a player's normal boost message should be active.
     * @param uuid The player's UUID.
     * @return true if the boost is active, false otherwise.
     */
    public boolean isBoostActive(UUID uuid) {
        Long expiryTime = boostMessageExpirations.get(uuid);
        if (expiryTime == null) {
            return false;
        }

        // Check if the current time has passed the expiry time
        if (System.currentTimeMillis() > expiryTime) {
            boostMessageExpirations.remove(uuid); // Clean up expired entry
            return false;
        }

        return true;
    }

    /**
     * Checks if a player's super boost message should be active.
     * @param uuid The player's UUID.
     * @return true if the super boost is active, false otherwise.
     */
    public boolean isSuperBoostActive(UUID uuid) {
        Long expiryTime = superBoostMessageExpirations.get(uuid);
        if (expiryTime == null) {
            return false;
        }

        if (System.currentTimeMillis() > expiryTime) {
            superBoostMessageExpirations.remove(uuid); // Clean up expired entry
            return false;
        }

        return true;
    }

    private void handleInAirBoost(Player player) {
        if (!configHandler.getIsBoostEnabled()) return;
        if (!PermissionsHelper.hasElytraBoostPermission(player) && !PermissionsHelper.hasElytraSuperBoostPermission(player)) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Material configuredMaterial = Material.valueOf(configHandler.getBoostItem());
        if (itemInHand.getType() != configuredMaterial) return;

        if (isOnCooldown(player)) return;

        PlayerStats stats = statsHandler.getStats(player);
        double boostMultiplier;
        boolean isSuperBoost = player.isSneaking();

        if (isSuperBoost) {
            if (!PermissionsHelper.hasElytraSuperBoostPermission(player)) return;
            stats.incrementSuperBoostsUsed();
            boostMultiplier = SUPER_BOOST_MULTIPLIER;
        } else {
            if (!PermissionsHelper.hasElytraBoostPermission(player)) return;
            stats.incrementBoostsUsed();
            boostMultiplier = BOOST_MULTIPLIER;
        }

        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        Vector direction = player.getLocation().getDirection();
        Vector boost = direction.multiply(boostMultiplier);
        player.setVelocity(player.getVelocity().add(boost));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);

        if (isSuperBoost) {
            superBoostMessageExpirations.put(player.getUniqueId(), System.currentTimeMillis() + 1000);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 40, 0.5, 0.5, 0.5, 0.1);
        } else {
            boostMessageExpirations.put(player.getUniqueId(), System.currentTimeMillis() + 1000);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void handleChargedJump(Player player) {
        if (!configHandler.getIsBoostEnabled() || !configHandler.getIsChargedJumpEnabled()) return;
        if (configHandler.getIsTimeLimitEnabled()){
            var currentTime = flightHandler.getCurrentFlightTime(player.getUniqueId());
            if (currentTime == 0){
                messagesHelper.sendActionBarMessage(player, messagesHandler.getElytraFlightTimeExpired());
                return;
            }
        }

        if (chargingTasks.containsKey(player.getUniqueId())) return; // Already charging

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) {
            return;
        }

        //  Permission Check
        if (!PermissionsHelper.hasChargedJumpPermission(player)) {
            return;
        }

        //  Item Check
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Material configuredMaterial = Material.valueOf(configHandler.getBoostItem());
        if (itemInHand.getType() != configuredMaterial) {
            return;
        }

        //  Cooldown Check
        if (isOnCooldown(player)) {
            return;
        }

        double chargeTimeSeconds = configHandler.getChargeTime();
        if (chargeTimeSeconds <= 0) {
            return;
        }

        long totalTicksToCharge = (long) (chargeTimeSeconds * 20);

        BossBar bossBar = plugin.getServer().createBossBar("§aCharging Jump... §20%", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        chargeBossBars.put(player.getUniqueId(), bossBar);

        var ticksElapsed = new AtomicLong(0); // Use AtomicLong for thread-safe incrementing

        CancellableTask task = foliaHelper.runTaskTimerForEntity(player, () -> {
            ItemStack currentItem = player.getInventory().getItemInMainHand();
            if (!player.isOnline() || !player.isSneaking() || !player.isOnGround() || currentItem.getType() != configuredMaterial) {
                cancelCharge(player);
                return;
            }

            long currentTick = ticksElapsed.incrementAndGet(); // Safely increment the counter
            double progress = (double) currentTick  / totalTicksToCharge;
            bossBar.setProgress(Math.min(1.0, progress));

            int percentage = (int) (progress * 100);
            bossBar.setTitle("§aCharging Jump... §2" + percentage + "%");

            if (progress > 0.75) {
                bossBar.setTitle("§cCharging Jump... §4" + percentage + "%");
                bossBar.setColor(BarColor.RED);
            } else if (progress > 0.4) {
                bossBar.setTitle("§eCharging Jump... §6" + percentage + "%");
                bossBar.setColor(BarColor.YELLOW);
            }

            // Play the charge-up effect if the player has an active effect
            playChargeUpEffect(player, currentTick , totalTicksToCharge);

            if (currentTick  % 4 == 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.5f, 1.5f);
            }

            if (currentTick  >= totalTicksToCharge) {
                messagesHelper.sendTitleMessage(player, "&r", "&#FFD700LAUNCH!", 5, 20, 10);

                double jumpStrength = configHandler.getJumpStrength();
                player.setVelocity(player.getVelocity().add(new Vector(0, jumpStrength, 0)));

                // Use Folia-safe delayed task
                foliaHelper.runTaskLater(player, () -> {
                    if (player.isOnline() && !player.isOnGround())
                    {
                        player.setGliding(true);
                        statsHandler.setGliding(player, true);
                        flightHandler.onGlideStartAttempt(player);
                    }
                }, 2L); //  we need this delay to ensure the player is airborne before setting gliding

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);

                if (serverVersion.ordinal() == ServerVersion.V_1_21.ordinal() ) {
                    player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 1);
                    player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                }
                else {
                    // For versions before 1.20 and below, use the old explosion particle
                    Particle particle = Particle.valueOf("EXPLOSION_NORMAL");
                    player.getWorld().spawnParticle(particle, player.getLocation(), 1);

                    Particle particle2 = Particle.valueOf("FIREWORKS_SPARK");
                    player.getWorld().spawnParticle(particle2, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                }

                cancelCharge(player);
            }
        }, 1L, 1L);

        chargingTasks.put(player.getUniqueId(), task);
    }

    private void cancelCharge(Player player) {
        CancellableTask task = chargingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        BossBar bossBar = chargeBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    /**
     * Checks if a player is on cooldown. Sends them a message if they are.
     * @param player The player to check.
     * @return True if the player is on cooldown, false otherwise.
     */
    private boolean isOnCooldown(Player player) {
        long effectiveCooldownMs;
        int configuredCooldownMs = configHandler.getBoostCooldown();

        if (PermissionsHelper.playerBypassBoostCooldown(player)) {
            // Players with bypass permission are still subject to the minimum anti-spam delay.
            effectiveCooldownMs = MINIMUM_SPAM_DELAY_MS;
        } else {
            // For normal players, use the configured value, but ensure it's never less than our minimum.
            effectiveCooldownMs = Math.max(configuredCooldownMs, MINIMUM_SPAM_DELAY_MS);
        }

        long lastBoostTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long timeSinceLastBoost = System.currentTimeMillis() - lastBoostTime;

        if (timeSinceLastBoost < effectiveCooldownMs) {
            // Player is on cooldown. Check if we should send a message.
            long lastMessageCooldownTime = cooldownMessageSent.getOrDefault(player.getUniqueId(), 0L);

            // We only send a message if it's for a new cooldown instance.
            // If the current cooldown start time is different from the last one we sent a message for,
            // it means the player has triggered a new cooldown and should be notified once.
            if (lastBoostTime != lastMessageCooldownTime) {
                // Only send the "wait X seconds" message if the cooldown is significant.
                if (effectiveCooldownMs > 1000) {
                    long remainingMs = effectiveCooldownMs - timeSinceLastBoost;
                    // Ensure we don't show "0 seconds".
                    int remainingSeconds = Math.max(1, (int) Math.ceil(remainingMs / 1000.0));

                    String messageTemplate = messagesHandler.getBoostCooldown();
                    String message = messageTemplate.replace("{0}", String.valueOf(remainingSeconds));
                    messagesHelper.sendPlayerMessage(player, message);

                    // Record that we've sent a message for this specific cooldown instance.
                    cooldownMessageSent.put(player.getUniqueId(), lastBoostTime);
                }
            }
            return true; // Player is on cooldown (message may or may not have been sent).
        }

        return false;
    }

    /**
     * Handles the visual particle effects for the charged jump.
     * Place this method within the same class that contains your handleChargedJump logic.
     */
    private void playChargeUpEffect(Player player, long ticksElapsed, long totalTicksToCharge) {
        // Configuration
        double radius = 1.0; // The radius of the particle circle.
        int particleCount = 20; // How many particles are in the circle at any time.
        double rotationSpeed = 0.1; // How fast the circle rotates.
        double maxRiseHeight = 1.5; // How high the particles will rise by the end of the charge.

        // Calculation
        Location playerLocation = player.getLocation();
        double progress = (double) ticksElapsed / totalTicksToCharge;

        // The angle offset makes the circle rotate over time.
        double angleOffset = ticksElapsed * rotationSpeed;
        // The y-offset makes the particles rise as the charge progresses.
        double yOffset = progress * maxRiseHeight;

        // Interpolate color from Green -> Yellow -> Red based on charge progress
        Color startColor;
        if (progress < 0.5) {
            // Green to Yellow (from 0% to 50% progress)
            float r = (float) (progress * 2); // Red component increases
            startColor = Color.fromRGB((int) (r * 255), 255, 0);
        } else {
            // Yellow to Red (from 50% to 100% progress)
            float g = (float) (1.0 - (progress - 0.5) * 2); // Green component decreases
            startColor = Color.fromRGB(255, (int) (g * 255), 0);
        }
        // The dust transition options allow for the color-changing effect.
        Particle.DustTransition dustOptions = new Particle.DustTransition(startColor, Color.RED, 1.0f);

        // Particle Spawning
        for (int i = 0; i < particleCount; i++) {
            double angle = 2 * Math.PI * i / particleCount + angleOffset;
            double x = playerLocation.getX() + radius * Math.cos(angle);
            double z = playerLocation.getZ() + radius * Math.sin(angle);
            Location particleLocation = new Location(player.getWorld(), x, playerLocation.getY() + yOffset, z);

            // Spawn the particle. Use DustTransition for the cool color-changing effect.
            player.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, particleLocation, 1, 0, 0, 0, 0, dustOptions, true);
        }
    }
}
