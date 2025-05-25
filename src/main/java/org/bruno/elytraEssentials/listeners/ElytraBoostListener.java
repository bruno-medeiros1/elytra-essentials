package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class ElytraBoostListener implements Listener {

    private final ElytraEssentials elytraEssentials;

    public ElytraBoostListener(ElytraEssentials elytraEssentials) {
        this.elytraEssentials = elytraEssentials;
    }

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final long cooldownTime = 0; // Cooldown in milliseconds (2 seconds)

    @EventHandler
    public void onFeatherRightClick(PlayerInteractEvent e) {

        Player player = e.getPlayer();

        // Check if the player right-clicked
        if (e.getHand() == EquipmentSlot.OFF_HAND) return; // Ignore off-hand
        if (!e.getAction().toString().contains("RIGHT_CLICK")) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if the item is a feather
        if (item.getType() != Material.FEATHER) return;

        // Check if the player is gliding (Elytra flying)
        if (!player.isGliding()) return;

        // Check cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long sinceLastBoost = 0;

        if (cooldowns.containsKey(playerId)){
            sinceLastBoost = currentTime - cooldowns.get(playerId);
            int remainingSeconds = (int) (cooldownTime - sinceLastBoost)/1000;
            if (sinceLastBoost < cooldownTime) {
                player.sendMessage("§cBoost is on cooldown! " + ChatColor.GOLD + remainingSeconds + " §csec remaining...");
                return;
            }
        }
        cooldowns.put(playerId, currentTime);

        // Apply the boost
        Vector direction = player.getLocation().getDirection().normalize();

        double maxSpeed = elytraEssentials.getConfig().getDouble("max_speed_kmh");

        player.setVelocity(direction.multiply(2.0)); // Adjust multiplier for boost strength
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);

        // Spawn particle trail
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 20 || !player.isGliding()) { // Run for 1 second (20 ticks)
                    this.cancel();
                    return;
                }

                // Spawn particles at the player's location
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 5, 0.2, 0.2, 0.2, 0.01);

                ticks++;
            }
        }.runTaskTimer(elytraEssentials, 0L, 1L); // Schedule every tick (1L)
    }
}
