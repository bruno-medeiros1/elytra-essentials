package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

//  TODO: Merge this listener into the ElytraFlightListener once we organize it better
public class EmergencyDeployListener implements Listener {

    private final ElytraEssentials plugin;

    // Tracks players who have recently had an auto-deploy to prevent spam.
    private final HashMap<UUID, Long> deployCooldowns = new HashMap<>();

    private static final long COOLDOWN_DURATION_MS = 5000;
    private static final int MIN_FALL_DISTANCE = 5;

    public EmergencyDeployListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Check if the feature is enabled in the config
        if (!plugin.getConfigHandlerInstance().getIsEmergencyDeployEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Check if the player is falling and not already gliding
        if (player.getVelocity().getY() >= 0 || player.isGliding() || player.isOnGround() || player.isInsideVehicle()) {
            return;
        }

        // Check if the player is on cooldown from a recent auto-deploy
        if (isOnCooldown(player)) {
            return;
        }

        // Check if the player has fallen far enough
        if (player.getFallDistance() < MIN_FALL_DISTANCE) {
            return;
        }

        // Check for permission
        if (!PermissionsHelper.hasAutoDeployPermission(player)) {
            return;
        }

        PlayerInventory inventory = player.getInventory();

        // Check if the player's chest slot is empty
        if (inventory.getChestplate() != null) {
            return;
        }

        // Search the player's main inventory for an elytra
        for (int i = 0; i < 36; i++) { // Main inventory slots are 0-35
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == Material.ELYTRA) {
                plugin.getLogger().info("Equipping elytra on emergency!");

                // Equip the elytra
                inventory.setChestplate(item.clone());
                item.setAmount(item.getAmount() - 1);

                // Activate gliding
                player.setGliding(true);
                player.setFallDistance(0);

                Vector launchDirection = player.getLocation().getDirection().setY(0).normalize();

                // Create a new velocity vector with a strong forward push and a slight upward lift.
                Vector launchVelocity = launchDirection.multiply(0.6).setY(0.5);

                // Set the player's velocity directly to this new vector to override their fall.
                player.setVelocity(launchVelocity);

                // Add player to cooldown to prevent this from firing again on the same fall
                deployCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_DURATION_MS);

                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getEmergencyDeploySuccess());
                break;
            }
        }
    }

    private boolean isOnCooldown(Player player) {
        if (!deployCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        if (System.currentTimeMillis() > deployCooldowns.get(player.getUniqueId())) {
            deployCooldowns.remove(player.getUniqueId());
            return false;
        }
        return true;
    }
}