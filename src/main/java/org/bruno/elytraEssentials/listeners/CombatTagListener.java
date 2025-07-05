package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CombatTagListener implements Listener {

    private final ElytraEssentials plugin;

    private final HashMap<UUID, Long> combatTaggedPlayers = new HashMap<>();
    private final Set<UUID> fallDamageProtection = new HashSet<>();
    private final HashMap<UUID, BossBar> combatTagBossBars = new HashMap<>();
    private BukkitTask countdownTask;

    public CombatTagListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!plugin.getConfigHandlerInstance().getIsCombatTagEnabled() || !player.isGliding()) {
            return;
        }

        if (PermissionsHelper.PlayerBypassCombatTag(player)) {
            return;
        }

        boolean playerDamageOnly = plugin.getConfigHandlerInstance().getIsCombatTagPlayerDamageOnlyEnabled();
        boolean damageSourceIsValid = false;

        if (playerDamageOnly) {
            if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
                Player damageSource = getPlayerDamager(damageByEntityEvent);
                if (damageSource != null && !damageSource.getUniqueId().equals(player.getUniqueId())) {
                    damageSourceIsValid = true;
                }
            }
        } else {
            if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
                Player damageSource = getPlayerDamager(damageByEntityEvent);
                if (damageSource != null && !damageSource.getUniqueId().equals(player.getUniqueId())) {
                    damageSourceIsValid = true;
                }
            } else {
                // Damage was not from an entity (e.g., fall, fire, cactus), so it's valid.
                damageSourceIsValid = true;
            }
        }

        if (damageSourceIsValid) {
            applyCombatTag(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlideAttempt(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!plugin.getConfigHandlerInstance().getIsCombatTagEnabled()) {
            return;
        }

        if (event.isGliding() && isCombatTagged(player)) {
            event.setCancelled(true);
            plugin.getMessagesHelper().sendActionBarMessage(player, "§cYou cannot glide while in combat!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!plugin.getConfigHandlerInstance().getIsCombatTagEnabled()) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && fallDamageProtection.remove(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        combatTaggedPlayers.remove(playerId);
        fallDamageProtection.remove(playerId);

        BossBar bossBar = combatTagBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void start() {
        if (!plugin.getConfigHandlerInstance().getIsCombatTagEnabled()) {
            return;
        }

        if (countdownTask != null && !countdownTask.isCancelled()) {
            return; // Task is already running
        }

        this.countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Use an iterator to safely remove from the map while looping
                Iterator<Map.Entry<UUID, Long>> iterator = combatTaggedPlayers.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<UUID, Long> entry = iterator.next();
                    UUID playerId = entry.getKey();
                    long expiryTime = entry.getValue();
                    Player player = Bukkit.getPlayer(playerId);
                    BossBar bossBar = combatTagBossBars.get(playerId);

                    if (player == null || !player.isOnline() || bossBar == null) {
                        // Clean up if player is offline or boss bar is missing
                        if (bossBar != null) bossBar.removeAll();
                        combatTagBossBars.remove(playerId);
                        iterator.remove();
                        continue;
                    }

                    long remainingMs = expiryTime - System.currentTimeMillis();
                    if (remainingMs > 0) {
                        // --- Update the BossBar ---
                        double progress = (double) remainingMs / (plugin.getConfigHandlerInstance().getCombatTagCooldown() * 1000L);
                        int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

                        bossBar.setProgress(Math.max(0, Math.min(1, progress)));
                        bossBar.setTitle("§cCombat Tagged. Time Left: §6" + TimeHelper.formatFlightTime(remainingSeconds));
                    } else {
                        // --- Tag has expired, clean everything up ---
                        bossBar.removeAll();
                        combatTagBossBars.remove(playerId);
                        iterator.remove(); // Remove from the combat tag map
                        plugin.getMessagesHelper().sendActionBarMessage(player, "§aYou can use your elytra again!");
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L); // Run every second
    }

    /**
     * Public method to cancel the task when the plugin disables.
     */
    public void cancel() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }

    private void applyCombatTag(Player player) {
        if (PermissionsHelper.PlayerBypassCombatTag(player)) {
            return;
        }

        player.setGliding(false);

        long durationMs = plugin.getConfigHandlerInstance().getCombatTagCooldown() * 1000L;
        combatTaggedPlayers.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);

        if (plugin.getConfigHandlerInstance().getIsCombatTagPreventFallDamageEnabled()) {
            fallDamageProtection.add(player.getUniqueId());
        }

        BossBar bossBar = combatTagBossBars.computeIfAbsent(player.getUniqueId(), uuid -> {
            BossBar newBar = Bukkit.createBossBar("Combat Tagged.", BarColor.RED, BarStyle.SOLID);
            newBar.addPlayer(player);
            return newBar;
        });

        plugin.getMessagesHelper().sendActionBarMessage(player, "§cYour elytra has been disabled due to combat!");
    }

    private boolean isCombatTagged(Player player) {
        Long expiryTime = combatTaggedPlayers.get(player.getUniqueId());
        return expiryTime != null && System.currentTimeMillis() < expiryTime;
    }

    private Player getPlayerDamager(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (damager instanceof Player p) {
            return p;
        }

        // Projectiles (covers Arrows, Tridents, Potions, etc.)
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }

        // Player-lit TNT
        if (damager instanceof org.bukkit.entity.TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            return p;
        }
        return null;
    }
}
