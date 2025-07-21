package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ArmoredElytraHelper;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArmoredElytraHandler {
    private final ElytraEssentials plugin;
    private final ConfigHandler configHandler;
    private final FoliaHelper foliaHelper;
    private final ArmoredElytraHelper armoredElytraHelper;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler;

    private final NamespacedKey armoredElytraKey;
    private final NamespacedKey toughnessElytraKey;
    private final NamespacedKey materialKey;
    private final NamespacedKey durabilityKey;
    private final NamespacedKey maxDurabilityKey;

    public ArmoredElytraHandler(ElytraEssentials plugin, ConfigHandler configHandler, FoliaHelper foliaHelper, ArmoredElytraHelper armoredElytraHelper,
                                MessagesHelper messagesHelper, MessagesHandler messagesHandler) {
        this.plugin = plugin;
        this.configHandler = configHandler;
        this.foliaHelper = foliaHelper;
        this.armoredElytraHelper = armoredElytraHelper;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;

        this.armoredElytraKey = new NamespacedKey(plugin, Constants.NBT.ARMORED_ELYTRA_TAG);
        this.toughnessElytraKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG);
        this.materialKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_MATERIAL_TAG);
        this.durabilityKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG);
        this.maxDurabilityKey = new NamespacedKey(plugin, Constants.NBT.MAX_ARMOR_DURABILITY_TAG);
    }

    /**
     * This is the new main entry point for handling damage events.
     * It contains all the logic from the old ArmoredElytraDamageListener.
     */
    public void handleDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!configHandler.getIsArmoredElytraEnabled()) return;

        ItemStack chestplate = player.getInventory().getChestplate();
        if (!isArmoredElytra(chestplate)) return;

        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int currentDurability = container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);

        if (currentDurability <= 0) return;

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
            messagesHelper.sendPlayerMessage(player, messagesHandler.getArmoredElytraBroken());
        }
    }

    /**
     * Schedules a check to update a player's armor attributes based on their equipped chestplate.
     * This is the main entry point called by listeners.
     */
    public void scheduleArmorCheck(Player player) {
        // Use the Folia-safe, entity-specific scheduler to run the check on the correct thread.
        foliaHelper.runTask(player, () -> checkAndApplyArmor(player));
    }

    /**
     * The core logic. Checks the player's chestplate and applies or removes attributes.
     */
    private void checkAndApplyArmor(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();

        // Always remove old attributes first to ensure a clean state.
        removeArmorAttributes(player);

        if (!configHandler.getIsArmoredElytraEnabled()) return;

        if (isArmoredElytra(chestplate)) {
            ItemMeta meta = chestplate.getItemMeta();
            if (meta == null) return;

            int currentDurability = meta.getPersistentDataContainer().getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);
            if (currentDurability > 0) {
                applyArmorAttributes(player, chestplate);
            }
        }
    }

    private void applyArmorAttributes(Player player, ItemStack armoredElytra) {
        Material armorType = getArmorMaterialFromNbt(armoredElytra);
        if (armorType == null) return;

        int armorPoints = getArmorPoints(armorType);
        int armorToughness = getArmorToughness(armorType);

        if (armorPoints > 0) {
            armoredElytraHelper.setArmorModifier(player, armoredElytraKey, armorPoints);
        }
        if (armorToughness > 0) {
            armoredElytraHelper.setToughnessModifier(player, toughnessElytraKey, armorToughness);
        }
    }

    public void removeArmorAttributes(Player player) {
        armoredElytraHelper.removeArmorModifier(player, armoredElytraKey);
        armoredElytraHelper.removeToughnessModifier(player, toughnessElytraKey);
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

    private boolean isArmoredElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(armoredElytraKey, PersistentDataType.BYTE);
    }

    private Material getArmorMaterialFromNbt(ItemStack armoredElytra) {
        if (armoredElytra == null || !armoredElytra.hasItemMeta()) return null;
        String materialName = Objects.requireNonNull(armoredElytra.getItemMeta())
                .getPersistentDataContainer()
                .get(materialKey, PersistentDataType.STRING);
        return materialName != null ? Material.matchMaterial(materialName) : null;
    }

    private int getArmorPoints(Material armorType) {
        return switch (armorType) {
            case LEATHER_CHESTPLATE -> 3;
            case CHAINMAIL_CHESTPLATE, GOLDEN_CHESTPLATE -> 5;
            case IRON_CHESTPLATE -> 6;
            case DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE -> 8;
            default -> 0;
        };
    }

    private int getArmorToughness(Material armorType) {
        return switch (armorType) {
            case DIAMOND_CHESTPLATE -> 2;
            case NETHERITE_CHESTPLATE -> 3;
            default -> 0;
        };
    }}
