package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
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
        double totalAbsorbed = container.getOrDefault(new NamespacedKey(plugin, Constants.NBT.DAMAGE_ABSORBED_TAG), PersistentDataType.DOUBLE, 0.0);
        container.set(new NamespacedKey(plugin, Constants.NBT.DAMAGE_ABSORBED_TAG), PersistentDataType.DOUBLE, totalAbsorbed + absorbed);

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
            player.sendMessage("§cYour Armored Elytra's plating has shattered!");
        }
    }

    private double applyAllProtection(double damage, PersistentDataContainer container, EntityDamageEvent.DamageCause cause) {

        // Each level reduces applicable damage by 4%. Capped at 80% total reduction from all sources.
        NamespacedKey protKey = new NamespacedKey(plugin, "chestplate_enchant_" + Enchantment.PROTECTION.getKey().getKey());
        int protectionLevel = container.getOrDefault(protKey, PersistentDataType.INTEGER, 0);
        damage = damage * (1.0 - (protectionLevel * 0.04));

        // Each level reduces that specific damage type by 8%.
        if (isFireDamage(cause)) {
            NamespacedKey fireProtKey = new NamespacedKey(plugin, "chestplate_enchant_" + Enchantment.FIRE_PROTECTION.getKey().getKey());
            int fireProtLevel = container.getOrDefault(fireProtKey, PersistentDataType.INTEGER, 0);
            damage = damage * (1.0 - (fireProtLevel * 0.08));
        } else if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            NamespacedKey blastProtKey = new NamespacedKey(plugin, "chestplate_enchant_" + Enchantment.BLAST_PROTECTION.getKey().getKey());
            int blastProtLevel = container.getOrDefault(blastProtKey, PersistentDataType.INTEGER, 0);
            damage = damage * (1.0 - (blastProtLevel * 0.08));
        } else if (cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            NamespacedKey projProtKey = new NamespacedKey(plugin, "chestplate_enchant_" + Enchantment.PROJECTILE_PROTECTION.getKey().getKey());
            int projProtLevel = container.getOrDefault(projProtKey, PersistentDataType.INTEGER, 0);
            damage = damage * (1.0 - (projProtLevel * 0.08));
        }

        return Math.max(0, damage); // Ensure damage never goes below zero.
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
        return meta.getPersistentDataContainer().has(armoredElytraKey, PersistentDataType.BOOLEAN);
    }

    private boolean isFireDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.LAVA ||
                cause == EntityDamageEvent.DamageCause.HOT_FLOOR;
    }
}
