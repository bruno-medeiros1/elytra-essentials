package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    // Cooldown
    private static final long MINIMUM_COOLDOWN_MS = 500; // A small internal cooldown to prevent spam

    // Boost strengths
    private static final double BOOST_MULTIPLIER = 0.5;
    private static final double SUPER_BOOST_MULTIPLIER = 1.0;

    private final ElytraEssentials plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

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

        // --- Cooldown Check ---
        if (isOnCooldown(player))
            return;

        PlayerStats stats = plugin.getStatsHandler().getStats(player);

        // --- Determine Boost Type and Apply ---
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

        // --- Apply Boost and Effects ---
        // Set the cooldown *after* all checks have passed
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Calculate and apply velocity
        Vector direction = player.getLocation().getDirection();
        Vector boost = direction.multiply(boostMultiplier);
        player.setVelocity(player.getVelocity().add(boost));

        //  TODO: Add visual indicator '+' for the speedometer upon boost usage
        //  TODO: fix playsound on older versions 1.18 and 1.19

        // Play sound effect
        //playSound(player);
    }

    /**
     * Checks if a player is on cooldown. Sends them a message if they are.
     * @param player The player to check.
     * @return True if the player is on cooldown, false otherwise.
     */
    private boolean isOnCooldown(Player player) {
        int configuredCooldownMs = plugin.getConfigHandlerInstance().getBoostCooldown();
        if (configuredCooldownMs <= MINIMUM_COOLDOWN_MS) {
            return false; // Cooldown is disabled or too short in config.
        }

        // Bypass permission check
        if (PermissionsHelper.PlayerBypassBoostCooldown(player)) {
            return false;
        }

        long lastBoostTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long timeSinceLastBoost = System.currentTimeMillis() - lastBoostTime;

        if (timeSinceLastBoost < configuredCooldownMs) {
            // Player is on cooldown, send them the message
            long remainingMs = configuredCooldownMs - timeSinceLastBoost;
            int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

            String messageTemplate = ChatColor.translateAlternateColorCodes('&', plugin.getMessagesHandlerInstance().getElytraBoostCooldown());
            String message = messageTemplate.replace("{0}", String.valueOf(remainingSeconds));
            plugin.getMessagesHelper().sendPlayerMessage(player, message);
            return true;
        }

        return false;
    }

    /**
     * Plays the configured boost sound for a player.
     * @param player The player to play the sound for.
     */
    private void playSound(Player player) {
        String soundName = plugin.getConfigHandlerInstance().getBoostSound().toUpperCase();
        try {
            Sound sound = Sound.valueOf(soundName);
            player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.2f); // Slightly higher pitch for a "boost" feel
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid sound name '" + soundName + "' in config.yml for the boost sound.");
        }
    }
}