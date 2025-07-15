package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ArmoredElytraDamageListener implements Listener {
    private final ElytraEssentials plugin;
    private final NamespacedKey armoredElytraKey;
    private final NamespacedKey durabilityKey;
    private final NamespacedKey maxDurabilityKey;

    public ArmoredElytraDamageListener(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.armoredElytraKey = new NamespacedKey(plugin, Constants.NBT.ARMORED_ELYTRA_TAG);
        this.durabilityKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG);
        this.maxDurabilityKey = new NamespacedKey(plugin, Constants.NBT.MAX_ARMOR_DURABILITY_TAG);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled()) {
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (!isArmoredElytra(chestplate)) {
            return;
        }

        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int currentDurability = container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);

        // If armor plating is broken, it offers no protection.
        if (currentDurability <= 0) return;

        //  Damage absorption logic from original chestplate enchantments
        double initialDamage = event.getDamage();
        double finalDamage = applyAllProtection(initialDamage, container, event.getCause());

        event.setDamage(finalDamage);

        double absorbed = initialDamage - finalDamage;
        double totalAbsorbed = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.DAMAGE_ABSORBED_TAG), PersistentDataType.FLOAT, 0.0f);
        container.set(new NamespacedKey(plugin, Constants.NBT.DAMAGE_ABSORBED_TAG), PersistentDataType.FLOAT, (float) (totalAbsorbed + absorbed));

        //  Handle Armor Durability
        //  TODO: Improve durability to not be so narrow to just removing 1 point
        int newDurability = currentDurability - 1;
        container.set(durabilityKey, PersistentDataType.INTEGER, newDurability);

        // Update the item's lore to show the new durability
        updateDurabilityLore(meta);
        chestplate.setItemMeta(meta);

        // Armor plating broke on this hit!
        if (newDurability <= 0) {
            int shatteredCount = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.PLATING_SHATTERED_TAG), PersistentDataType.INTEGER, 0);
            container.set(new NamespacedKey(plugin, Constants.NBT.PLATING_SHATTERED_TAG), PersistentDataType.INTEGER, shatteredCount + 1);

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            plugin.getMessagesHelper().sendPlayerMessage(player, "&cYour Armored Elytra's plating has shattered!");
        }
    }

    private double applyAllProtection(double damage, PersistentDataContainer container, EntityDamageEvent.DamageCause cause) {
        //  Assuming the enchantment names don't change in future versions.
        String protectionKey = resolveProtectionKeyAlias("PROTECTION");
        String fireProtKeyStr = resolveProtectionKeyAlias("FIRE_PROTECTION");
        String blastProtKeyStr = resolveProtectionKeyAlias("BLAST_PROTECTION");
        String projProtKeyStr = resolveProtectionKeyAlias("PROJECTILE_PROTECTION");

        //  Since the enchantments are only possible on chestplates,
        //  we can safely use the "chestplate_enchant_" prefix.
        int protectionLevel = getLevel(container, "chestplate_enchant_" + protectionKey);
        int fireProtLevel = getLevel(container, "chestplate_enchant_" + fireProtKeyStr);
        int blastProtLevel = getLevel(container, "chestplate_enchant_" + blastProtKeyStr);
        int projProtLevel = getLevel(container, "chestplate_enchant_" + projProtKeyStr);

        double reduction = 0.0;

        // Fire Protection: 15% less fire damage per level
        if (isFireDamage(cause)) {
            reduction += fireProtLevel * 0.15;
        }

        // Blast Protection: 15% less from explosions per level
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            reduction += blastProtLevel * 0.15;
        }

        // Projectile Protection: 15% less projectile damage per level
        if (cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            reduction += projProtLevel * 0.15;
        }

        // Protection: 4% reduction for *any* damage type per level
        reduction += protectionLevel * 0.04;

        // Cap the total damage reduction at 80% (Minecraft vanilla rule)
        return damage * (1 - Math.min(reduction, 0.8));
    }

    private void updateDurabilityLore(ItemMeta meta) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        int current = container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);
        int max = container.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, 1);

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        double percentage = (max > 0) ? (double) current / max : 0;
        String durabilityColor;
        if (percentage == 1){
            durabilityColor = "§a";
        } else if (percentage > 0.5) {
            durabilityColor = "§e";
        } else if (percentage > 0.2) {
            durabilityColor = "§c";
        } else {
            durabilityColor = "§4";
        }
        String durabilityLine = String.format("§6Armor Plating: %s%d §7/ §a%d", durabilityColor, current, max);

        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains("Armor Plating:")) {
                lore.set(i, durabilityLine);
                found = true;
                break;
            }
        }
        if (!found) { // Should not happen, but a good safety check.
            lore.add(durabilityLine);
        }

        meta.setLore(lore);
    }

    private boolean isArmoredElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(armoredElytraKey, PersistentDataType.BYTE);
    }

    private boolean isFireDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.LAVA ||
                cause == EntityDamageEvent.DamageCause.HOT_FLOOR;
    }

    private String resolveProtectionKeyAlias(String key) {
        if (ServerVersion.getCurrent().equals(ServerVersion.V_1_18) || ServerVersion.getCurrent().equals(ServerVersion.V_1_19)
                || ServerVersion.getCurrent().equals(ServerVersion.V_1_20))
        {
            return switch (key) {
                case "PROTECTION" -> "PROTECTION_ENVIRONMENTAL";
                case "FIRE_PROTECTION" -> "PROTECTION_FIRE";
                case "BLAST_PROTECTION" -> "PROTECTION_EXPLOSIONS";
                case "PROJECTILE_PROTECTION" -> "PROTECTION_PROJECTILE";
                default -> key;
            };
        }
        return key; // For 1.21+, use new names
    }

    private int getLevel(PersistentDataContainer container, String key) {
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key.toLowerCase()); // Normalize if needed
        return container.getOrDefault(namespacedKey, PersistentDataType.INTEGER, 0);
    }
}
