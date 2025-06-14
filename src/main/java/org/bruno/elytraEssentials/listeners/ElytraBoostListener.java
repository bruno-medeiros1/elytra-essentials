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

    private final ElytraEssentials plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    public ElytraBoostListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemBoostRightClick(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        // Check if the player right-clicked
        if (e.getHand() == EquipmentSlot.OFF_HAND) return; // Ignore off-hand
        if (!e.getAction().toString().contains("RIGHT_CLICK")) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        // Get configured boost item and cooldown
        String configuredBoostMaterial = plugin.getConfigHandlerInstance().getBoostItem();
        Material configuredMaterial = Material.matchMaterial(configuredBoostMaterial);
        if (configuredMaterial == null) {
            plugin.getLogger().warning("Invalid item material " + configuredBoostMaterial + " in config.yml for the boost item");
            return;
        }
        int cooldownTime = plugin.getConfigHandlerInstance().getBoostCooldown();

        // Check if the item matches the configured material
        if (item.getType() != configuredMaterial) return;

        // Check if the player is gliding (Elytra flying)
        if (!player.isGliding()) return;

        // Check cooldown
        boolean playerBypassBoostCooldown = PlayerBypassBoostCooldown(player);
        if (!playerBypassBoostCooldown ) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            long sinceLastBoost;

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

        // Apply the boost
        Vector direction = player.getLocation().getDirection().normalize();
        Vector currentVelocity = player.getVelocity();
        Vector boost = direction.multiply(1.5).add(currentVelocity);
        player.setVelocity(boost);

        //  Play sound
        String configuredSoundName = plugin.getConfigHandlerInstance().getBoostSound().toUpperCase();
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
}
