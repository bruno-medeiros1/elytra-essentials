package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class ElytraBoostListener implements Listener {
    private static final long MINIMUM_SPAM_DELAY_MS = 50; // The absolute minimum cooldown
    private static final double BOOST_MULTIPLIER = 0.5;
    private static final double SUPER_BOOST_MULTIPLIER = 1.0;

    private final ElytraEssentials plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    private final HashMap<UUID, Long> superBoostMessageExpirations = new HashMap<>();
    private final HashMap<UUID, Long> boostMessageExpirations = new HashMap<>();

    private final HashMap<UUID, BukkitTask> chargingTasks = new HashMap<>();
    private final HashMap<UUID, BossBar> chargeBossBars = new HashMap<>();

    public ElytraBoostListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        if (player.isGliding()) {
            handleInAirBoost(player);
        } else if (player.isSneaking() && player.isOnGround()) {
            handleChargedJump(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up all maps when a player leaves
        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.remove(playerId);
        boostMessageExpirations.remove(playerId);
        superBoostMessageExpirations.remove(playerId);
        cancelCharge(event.getPlayer()); // Cancel any active charge
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Cancel the charge if a player stops sneaking
        if (!event.isSneaking() && chargingTasks.containsKey(player.getUniqueId())) {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
            cancelCharge(player);
        }
    }

    private void handleInAirBoost(Player player) {
        if (!plugin.getConfigHandlerInstance().getIsBoostEnabled()) return;
        if (!PermissionsHelper.hasElytraBoostPermission(player) && !PermissionsHelper.hasElytraSuperBoostPermission(player)) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Material configuredMaterial = Material.valueOf(plugin.getConfigHandlerInstance().getBoostItem());
        if (itemInHand.getType() != configuredMaterial) return;

        if (isOnCooldown(player)) return;

        PlayerStats stats = plugin.getStatsHandler().getStats(player);
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
        if (!plugin.getConfigHandlerInstance().getIsBoostEnabled() || !plugin.getConfigHandlerInstance().getIsChargedJumpEnabled()) return;
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
        Material configuredMaterial = Material.valueOf(plugin.getConfigHandlerInstance().getBoostItem());
        if (itemInHand.getType() != configuredMaterial) {
            return;
        }

        //  Cooldown Check
        if (isOnCooldown(player)) {
            return;
        }

        double chargeTimeSeconds = plugin.getConfigHandlerInstance().getChargeTime();
        if (chargeTimeSeconds <= 0) {
            return;
        }

        long totalTicksToCharge = (long) (chargeTimeSeconds * 20);

        BossBar bossBar = plugin.getServer().createBossBar("§aCharging Jump... §20%", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        chargeBossBars.put(player.getUniqueId(), bossBar);

        BukkitTask task = new BukkitRunnable() {
            private long ticksElapsed = 0;

            @Override
            public void run() {
                ItemStack currentItem = player.getInventory().getItemInMainHand();
                if (!player.isOnline() || !player.isSneaking() || !player.isOnGround() || currentItem.getType() != configuredMaterial) {
                    cancelCharge(player);
                    return;
                }

                ticksElapsed++;
                double progress = (double) ticksElapsed / totalTicksToCharge;
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

                if (ticksElapsed % 4 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.5f, 1.5f);
                }

                if (ticksElapsed >= totalTicksToCharge) {
                    plugin.getMessagesHelper().sendTitleMessage(player, "&r", "&#FFD700LAUNCH!", 5, 20, 10);

                    double jumpStrength = plugin.getConfigHandlerInstance().getJumpStrength();
                    player.setVelocity(player.getVelocity().add(new Vector(0, jumpStrength, 0)));

                    //  Schedule the glide to activate with a 2-tick delay.
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && !player.isOnGround()) {
                                player.setGliding(true);

                                // Makes sure we trigger the glide start event
                                ElytraFlightListener flightListener = plugin.getElytraFlightListener();
                                if (flightListener != null) {
                                    flightListener.handleGlideStart(player);
                                }
                            }
                        }
                    }.runTaskLater(plugin, 2L);

                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);

                    if (plugin.getServerVersion().ordinal() == ServerVersion.V_1_21.ordinal() ) {
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
            }
        }.runTaskTimer(plugin, 0L, 1L);

        chargingTasks.put(player.getUniqueId(), task);
    }

    private void cancelCharge(Player player) {
        BukkitTask task = chargingTasks.remove(player.getUniqueId());
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
        int configuredCooldownMs = plugin.getConfigHandlerInstance().getBoostCooldown();

        if (PermissionsHelper.PlayerBypassBoostCooldown(player)) {
            // Players with bypass permission are still subject to the minimum anti-spam delay.
            effectiveCooldownMs = MINIMUM_SPAM_DELAY_MS;
        } else {
            // For normal players, use the configured value, but ensure it's never less than our minimum.
            effectiveCooldownMs = Math.max(configuredCooldownMs, MINIMUM_SPAM_DELAY_MS);
        }

        long lastBoostTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long timeSinceLastBoost = System.currentTimeMillis() - lastBoostTime;

        if (timeSinceLastBoost < effectiveCooldownMs) {
            // Player is on cooldown, send them the message

            // Only send the "wait X seconds" message if the cooldown is significant (e.g., > 1 second)
            if (effectiveCooldownMs > 1000) {
                long remainingMs = effectiveCooldownMs - timeSinceLastBoost;
                int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

                String messageTemplate = plugin.getMessagesHandlerInstance().getBoostCooldown();
                String message = messageTemplate.replace("{0}", String.valueOf(remainingSeconds));
                plugin.getMessagesHelper().sendPlayerMessage(player, message);
            }
            return true; // Player is on cooldown
        }

        return false;
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
}