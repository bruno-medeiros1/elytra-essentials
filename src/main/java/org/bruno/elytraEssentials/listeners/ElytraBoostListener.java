package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class ElytraBoostListener implements Listener {

    private static final long MINIMUM_COOLDOWN = 100; // 100 milliseconds

    //  TODO: Review if this make sense to add as properties in the config
    private static final double BOOST_MULTIPLIER = 0.2;

    private final ElytraEssentials plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    public ElytraBoostListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemBoostRightClick(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!player.isGliding()) return;

        // Check if the player right-clicked
        if (e.getHand() == EquipmentSlot.OFF_HAND) return; // Ignore off-hand
        if (!e.getAction().toString().contains("RIGHT_CLICK")) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        // Get configured boost item
        String configuredBoostMaterial = plugin.getConfigHandlerInstance().getBoostItem();
        Material configuredMaterial = Material.matchMaterial(configuredBoostMaterial);
        if (configuredMaterial == null) {
            plugin.getLogger().warning("Invalid item material " + configuredBoostMaterial + " in config.yml for the boost item");
            return;
        }

        // Check if the item matches the configured material
        if (item.getType() != configuredMaterial) return;


        //  cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long sinceLastBoost;

        int cooldownTime = plugin.getConfigHandlerInstance().getBoostCooldown();
        if (cooldownTime > MINIMUM_COOLDOWN) {
            boolean playerBypassBoostCooldown = PlayerBypassBoostCooldown(player);
            if (!playerBypassBoostCooldown ) {
                if (cooldowns.containsKey(playerId)) {
                    sinceLastBoost = currentTime - cooldowns.get(playerId);
                    int remainingSeconds = (int) (cooldownTime - sinceLastBoost) / 1000;
                    if (sinceLastBoost < cooldownTime) {

                        //  format message
                        String boostCooldownMessageTemplate = ChatColor.translateAlternateColorCodes('&',
                                plugin.getMessagesHandlerInstance().getElytraBoostCooldown());
                        String boostCooldownMessage = boostCooldownMessageTemplate.replace("{0}", String.valueOf(remainingSeconds));
                        plugin.getMessagesHelper().sendPlayerMessage(player, boostCooldownMessage);

                        return;
                    }
                }
                cooldowns.put(playerId, currentTime);
            }
        }
        else{
            if (cooldowns.containsKey(playerId)) {
                sinceLastBoost = currentTime - cooldowns.get(playerId);
                if (sinceLastBoost < MINIMUM_COOLDOWN) {
                    return; // Prevent spamming even for bypass players
                }
            }
            cooldowns.put(playerId, currentTime);
        }

        //  velocity calculation
        Vector direction = player.getLocation().getDirection().normalize();
        Vector currentVelocity = player.getVelocity();
        Vector boost = direction.multiply(BOOST_MULTIPLIER).add(currentVelocity);

        // Apply the boost
        player.setVelocity(boost);

        //  Play sound
        String configuredSoundName = plugin.getConfigHandlerInstance().getBoostSound().toUpperCase();
        if (configuredSoundName.isBlank())
            return;

        Sound sound = null;
        try {
            sound = Sound.valueOf(configuredSoundName);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid sound name " + configuredSoundName + " in config.yml for the boost sound");
        }

        if (sound != null) {
            player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    private boolean PlayerBypassBoostCooldown(Player player) {
        return player.hasPermission("elytraessentials.bypass.boostcooldown") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                player.hasPermission("elytraessentials.*");
    }

    private boolean PlayerBypassSpeedLimit(Player player) {
        return player.hasPermission("elytraessentials.bypass.speedlimit") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                player.hasPermission("elytraessentials.*");
    }
}
