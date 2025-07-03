package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class ElytraBoostListener implements Listener {
    private static final long MINIMUM_SPAM_DELAY_MS = 50; // The absolute minimum cooldown

    // Boost strengths
    private static final double BOOST_MULTIPLIER = 0.5;
    private static final double SUPER_BOOST_MULTIPLIER = 1.0;

    private final ElytraEssentials plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    private final HashMap<UUID, Long> boostMessageExpirations = new HashMap<>();
    private final HashMap<UUID, Long> superBoostMessageExpirations = new HashMap<>();

    public ElytraBoostListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemBoostRightClick(PlayerInteractEvent e) {
        if (!plugin.getConfigHandlerInstance().getIsBoostEnabled())
            return;

        if (e.getHand() == EquipmentSlot.OFF_HAND) return; // Ignore off-hand actions

        Player player = e.getPlayer();
        if (!player.isGliding()) return;

        // Ensure it's a right-click action
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // Check if the player has permission for at least one of the boosts
        if (!PermissionsHelper.hasElytraBoostPermission(player) && !PermissionsHelper.hasElytraSuperBoostPermission(player)) return;

        // Check for the configured boost item
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        String configuredBoostMaterial = plugin.getConfigHandlerInstance().getBoostItem();
        Material configuredMaterial = Material.matchMaterial(configuredBoostMaterial);
        if (configuredMaterial == null || itemInHand.getType() != configuredMaterial)
            return;

        //  Cooldown Check
        if (isOnCooldown(player))
            return;

        PlayerStats stats = plugin.getStatsHandler().getStats(player);

        //  Determine Boost Type and Apply
        double boostMultiplier;
        boolean isSuperBoost = player.isSneaking();

        if (isSuperBoost) {
            if (!PermissionsHelper.hasElytraSuperBoostPermission(player))
                return;

            stats.incrementSuperBoostsUsed();
            boostMultiplier = SUPER_BOOST_MULTIPLIER;
        } else {
            if (!PermissionsHelper.hasElytraBoostPermission(player))
                return;

            stats.incrementBoostsUsed();
            boostMultiplier = BOOST_MULTIPLIER;
        }

        // Set the cooldown *after* all checks have passed
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Calculate and apply velocity
        Vector direction = player.getLocation().getDirection();
        Vector boost = direction.multiply(boostMultiplier);
        player.setVelocity(player.getVelocity().add(boost));

        // Play sound effect
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f); // Slightly higher pitch for a "boost" feel

        //  Display boost action bar message temporarily for 1s and spawn impulse effect
        if (isSuperBoost){
            superBoostMessageExpirations.put(player.getUniqueId(), System.currentTimeMillis() + 1000);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(),
                    40, 0.5, 0.5, 0.5, 0.1);
        }
        else {
            boostMessageExpirations.put(player.getUniqueId(), System.currentTimeMillis() + 1000);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(),
                    20, 0.5, 0.5, 0.5, 0.1);
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

    public HashMap<UUID, Long> getBoostMessageExpirations(){
        return this.boostMessageExpirations;
    }

    public HashMap<UUID, Long> getSuperBoostMessageExpirations(){
        return this.superBoostMessageExpirations;
    }
}