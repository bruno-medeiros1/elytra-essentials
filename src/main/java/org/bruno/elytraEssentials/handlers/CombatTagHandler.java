package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.*;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTagHandler {
    private final ConfigHandler configHandler;
    private final MessagesHelper messagesHelper;
    private final FoliaHelper foliaHelper;
    private final MessagesHandler messagesHandler;

    private final Map<UUID, Long> combatTaggedPlayers = new ConcurrentHashMap<>();
    private final Set<UUID> fallDamageProtection = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BossBar> combatTagBossBars = new ConcurrentHashMap<>();
    private CancellableTask countdownTask;

    public CombatTagHandler(ConfigHandler configHandler, MessagesHelper messagesHelper, FoliaHelper foliaHelper, MessagesHandler messagesHandler) {
        this.configHandler = configHandler;
        this.messagesHelper = messagesHelper;
        this.foliaHelper = foliaHelper;
        this.messagesHandler = messagesHandler;
    }

    public void start() {
        if (!configHandler.getIsCombatTagEnabled() || countdownTask != null) return;

        // Use the Folia-safe global timer. The logic inside accesses Bukkit APIs (getPlayer),
        // so it should run on a main server thread.
        this.countdownTask = foliaHelper.runTaskTimerGlobal(() -> {
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
                    // Update the BossBar
                    double progress = (double) remainingMs / (configHandler.getCombatTagCooldown() * 1000L);
                    int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

                    bossBar.setProgress(Math.max(0, Math.min(1, progress)));
                    bossBar.setTitle(ColorHelper.parse(messagesHandler.getCombatTagged().replace("{0}", TimeHelper.formatFlightTime(remainingSeconds))));
                } else {
                    // Tag has expired, clean everything up
                    bossBar.removeAll();
                    combatTagBossBars.remove(playerId);
                    iterator.remove();

                    messagesHelper.sendActionBarMessage(player, messagesHandler.getCombatTaggedExpired());
                }
            }
        }, 20L, 20L); // Run every second
    }

    public void shutdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        // Clean up any remaining boss bars
        combatTagBossBars.values().forEach(BossBar::removeAll);
        combatTagBossBars.clear();
    }

    public void handleDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !configHandler.getIsCombatTagEnabled() || !player.isGliding()) return;
        if (PermissionsHelper.playerBypassCombatTag(player)) return;

        boolean playerDamageOnly = configHandler.getIsCombatTagPlayerDamageOnlyEnabled();
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

    public void handleGlideAttempt(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player) || !configHandler.getIsCombatTagEnabled()) return;

        if (event.isGliding() && isCombatTagged(player)) {
            event.setCancelled(true);
            messagesHelper.sendActionBarMessage(player, messagesHandler.getCannotGlideCombatTagged());
        }
    }

    public void handleFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !configHandler.getIsCombatTagEnabled()) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && fallDamageProtection.remove(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    public void handlePlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        combatTaggedPlayers.remove(playerId);
        fallDamageProtection.remove(playerId);

        BossBar bossBar = combatTagBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    private void applyCombatTag(Player player) {
        if (PermissionsHelper.playerBypassCombatTag(player)) {
            return;
        }

        player.setGliding(false);

        long durationMs = configHandler.getCombatTagCooldown() * 1000L;
        combatTaggedPlayers.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);

        if (configHandler.getIsCombatTagPreventFallDamageEnabled()) {
            fallDamageProtection.add(player.getUniqueId());
        }

        BossBar bossBar = combatTagBossBars.computeIfAbsent(player.getUniqueId(), uuid -> {
            BossBar newBar = Bukkit.createBossBar("Combat Tagged.", BarColor.RED, BarStyle.SOLID);
            newBar.addPlayer(player);
            return newBar;
        });

        messagesHelper.sendActionBarMessage(player, "§cYour elytra has been disabled due to combat!");
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
