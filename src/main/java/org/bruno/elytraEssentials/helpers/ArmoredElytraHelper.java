package org.bruno.elytraEssentials.helpers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A helper class responsible for all logic related to creating,
 * reverting, and identifying Armored Elytras.
 */
public class ArmoredElytraHelper {

    private final ElytraEssentials plugin;
    private final NamespacedKey armoredElytraKey;
    private final NamespacedKey materialKey;
    private final NamespacedKey previewItemKey;
    private final NamespacedKey durabilityKey;
    private final NamespacedKey maxDurabilityKey;

    public ArmoredElytraHelper(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.armoredElytraKey = new NamespacedKey(plugin, Constants.NBT.ARMORED_ELYTRA_TAG);
        this.materialKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_MATERIAL_TAG);
        this.previewItemKey = new NamespacedKey(plugin, Constants.NBT.PREVIEW_ITEM_TAG);
        this.durabilityKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG);
        this.maxDurabilityKey = new NamespacedKey(plugin, Constants.NBT.MAX_ARMOR_DURABILITY_TAG);
    }

    public ItemStack createArmoredElytra(ItemStack elytra, ItemStack chestplate, Player player) {
        ItemStack armoredElytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = armoredElytra.getItemMeta();

        if (elytra.getItemMeta() instanceof Damageable sourceDamage && meta instanceof Damageable targetDamage) {
            targetDamage.setDamage(sourceDamage.getDamage());
        }

        Material armorType = chestplate.getType();
        int maxArmorDurability = armorType.getMaxDurability();
        int currentArmorDurability = maxArmorDurability;
        if (chestplate.getItemMeta() instanceof Damageable sourceArmorDamage) {
            currentArmorDurability = maxArmorDurability - sourceArmorDamage.getDamage();
        }

        int armorPoints = getArmorPoints(armorType);
        int armorToughness = getArmorToughness(armorType);

        if (meta != null) {
            meta.setDisplayName("§b§lArmored Elytra");
            List<String> lore = new ArrayList<>();
            lore.add("§7A fusion of flight and protection.");
            lore.add("");
            lore.add("§6Armor Stats:");
            lore.add(String.format(" §f- §7Armor: §a+%d", armorPoints));
            if (armorToughness > 0) {
                lore.add(String.format(" §f- §7Armor Toughness: §a+%d", armorToughness));
            }
            lore.add("");
            lore.add(String.format("§6Armor Plating: §a%d / %d", currentArmorDurability, maxArmorDurability));

            Map<Enchantment, Integer> displayEnchants = new HashMap<>(chestplate.getEnchantments());
            elytra.getEnchantments().forEach((enchant, level) -> displayEnchants.merge(enchant, level, Integer::max));

            if (!displayEnchants.isEmpty()) {
                lore.add("");
                lore.add("§dEnchantments:");
            }
            for (Map.Entry<Enchantment, Integer> entry : displayEnchants.entrySet()) {
                lore.add(String.format(" §f- §7%s %d", getCapitalizedName(entry.getKey().getKey().getKey()), entry.getValue()));
            }
            meta.setLore(lore);

            elytra.getEnchantments().forEach((enchant, level) -> {
                String normalizedKey = getStandardEnchantmentKey(enchant);
                NamespacedKey key = new NamespacedKey(plugin, "elytra_enchant_" + normalizedKey);
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);
            });

            chestplate.getEnchantments().forEach((enchant, level) -> {
                String normalizedKey = getStandardEnchantmentKey(enchant);
                NamespacedKey key = new NamespacedKey(plugin, "chestplate_enchant_" + normalizedKey);
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);
            });

            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            meta.getPersistentDataContainer().set(armoredElytraKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, currentArmorDurability);
            meta.getPersistentDataContainer().set(maxDurabilityKey, PersistentDataType.INTEGER, maxArmorDurability);
            meta.getPersistentDataContainer().set(materialKey, PersistentDataType.STRING, chestplate.getType().name());
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.FORGED_BY_TAG), PersistentDataType.STRING, player.getName());
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.DAMAGE_ABSORBED_TAG), PersistentDataType.FLOAT, 0.0f);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.PLATING_SHATTERED_TAG), PersistentDataType.INTEGER, 0);

            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.PREVIEW_ITEM_TAG), PersistentDataType.BYTE, (byte) 1);

            armoredElytra.setItemMeta(meta);
        }
        return armoredElytra;
    }

    public ItemStack reassembleChestplate(ItemStack armoredElytra) {
        ItemMeta sourceMeta = armoredElytra.getItemMeta();
        if (sourceMeta == null) return null;

        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();

        String materialName = container.get(materialKey, PersistentDataType.STRING);
        Material armorType = (materialName != null) ? Material.matchMaterial(materialName) : Material.DIAMOND_CHESTPLATE;
        ItemStack chestplate = new ItemStack(armorType);
        ItemMeta chestMeta = chestplate.getItemMeta();

        if (chestMeta != null) {

            //  Transfer armor durability back to the chestplate
            if (chestMeta instanceof Damageable targetDamage) {
                int max = container.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, 1);
                int current = container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);
                targetDamage.setDamage(max - current);
            }

            // Loop through all enchantments and look for ones stored with the "chestplate_" prefix
            for (Enchantment enchantment : Enchantment.values()) {
                NamespacedKey key = new NamespacedKey(plugin, "chestplate_enchant_" + enchantment.getKey().getKey());
                if (container.has(key, PersistentDataType.INTEGER)) {
                    int level = container.get(key, PersistentDataType.INTEGER);
                    chestMeta.addEnchant(enchantment, level, true);
                }
            }
            chestMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.PREVIEW_ITEM_TAG), PersistentDataType.BYTE, (byte) 1);

            chestplate.setItemMeta(chestMeta);
        }
        return chestplate;
    }


    public ItemStack reassembleElytra(ItemStack armoredElytra) {
        ItemMeta sourceMeta = armoredElytra.getItemMeta();
        if (sourceMeta == null) return null;

        PersistentDataContainer container = sourceMeta.getPersistentDataContainer();
        ItemStack plainElytra = new ItemStack(Material.ELYTRA);
        ItemMeta elytraMeta = plainElytra.getItemMeta();

        if (elytraMeta instanceof Damageable targetDamage) {
            if (sourceMeta instanceof Damageable sourceDamage) {
                targetDamage.setDamage(sourceDamage.getDamage());
            }
        }

        if (elytraMeta != null) {
            // Loop through all enchantments and look for ones stored with the "elytra_" prefix
            for (Enchantment enchantment : Enchantment.values()) {
                NamespacedKey key = new NamespacedKey(plugin, "elytra_enchant_" + enchantment.getKey().getKey());
                if (container.has(key, PersistentDataType.INTEGER)) {
                    int level = container.get(key, PersistentDataType.INTEGER);
                    elytraMeta.addEnchant(enchantment, level, true);
                }
            }
            elytraMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, Constants.NBT.PREVIEW_ITEM_TAG), PersistentDataType.BYTE, (byte) 1);

            plainElytra.setItemMeta(elytraMeta);
        }
        return plainElytra;
    }


    public void addPreviewTag(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(previewItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    public boolean isPreviewItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(previewItemKey, PersistentDataType.BYTE);
    }

    public ItemStack createCleanCopy(ItemStack item) {
        if (item == null) return null;
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(previewItemKey);
            copy.setItemMeta(meta);
        }
        return copy;
    }

    public boolean isChestplate(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("_CHESTPLATE");
    }

    public boolean isArmoredElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(armoredElytraKey, PersistentDataType.BYTE);
    }

    public boolean isPlainElytra(ItemStack item) {
        return item != null && item.getType() == Material.ELYTRA && !isArmoredElytra(item);
    }

    public AttributeInstance getArmorAttribute(Player player) {
        Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.armor"));

        if (attribute == null) {
            //  Fallback for versions that do not have the 'generic.armor' attribute (1.21.3+)
            AttributeInstance attributeInstance = player.getAttribute(Attribute.ARMOR);
            if (attributeInstance == null) {
                plugin.getLogger().warning("Player does not have 'armor' attribute instance. Report this to the plugin author!");
                return null;
            }
            return attributeInstance;
        }

        return player.getAttribute(attribute);
    }

    public AttributeInstance getToughnessAttribute(Player player) {
        Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.armor_toughness"));

        if (attribute == null) {
            AttributeInstance attributeInstance = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
            if (attributeInstance == null) {
                plugin.getLogger().warning("Player does not have 'armor_toughness' attribute instance. Report this to the plugin author!");
                return null;
            }
            return attributeInstance;
        }

        return player.getAttribute(attribute);
    }

    public void setArmorModifier(Player player, NamespacedKey key, int armorPoints) {
        AttributeInstance armorAttr = getArmorAttribute(player);
        if (armorAttr == null) return;

        AttributeModifier modifier = createModifier(key, armorPoints, "armor_modifier", AttributeModifier.Operation.ADD_NUMBER);
        if (modifier == null) return;

        boolean alreadyExists = false;
        try {
            Method getKeyMethod = AttributeModifier.class.getMethod("getKey");
            Object result = getKeyMethod.invoke(modifier);

            if (result instanceof NamespacedKey) {
                NamespacedKey modKey = (NamespacedKey) result;
                alreadyExists = armorAttr.getModifiers().stream()
                    .anyMatch(existing -> {
                        try {
                            Method existingGetKey = AttributeModifier.class.getMethod("getKey");
                            Object existingKeyObj = existingGetKey.invoke(existing);
                            return existingKeyObj instanceof NamespacedKey && existingKeyObj.equals(modKey);
                        } catch (Exception e) {
                            return false;
                        }
                    });
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            // Fallback to UUID (older versions)
            UUID legacyKey = toUUID(key);

            if (modifier.getUniqueId().equals(legacyKey)) {
                armorAttr.addModifier(modifier);
            }
        }

        if (!alreadyExists) {
            armorAttr.addModifier(modifier);
        }
    }

    public void setToughnessModifier(Player player, NamespacedKey key, int toughnessPoints) {
        AttributeInstance toughnessAttr = getToughnessAttribute(player);
        if (toughnessAttr == null) return;

        AttributeModifier modifier = createModifier(key, toughnessPoints, "toughness_modifier", AttributeModifier.Operation.ADD_NUMBER);
        if (modifier == null) return;

        boolean alreadyExists = false;

        try {
            Method getKeyMethod = AttributeModifier.class.getMethod("getKey");
            Object result = getKeyMethod.invoke(modifier);

            if (result instanceof NamespacedKey modKey) {
                alreadyExists = toughnessAttr.getModifiers().stream().anyMatch(existing -> {
                    try {
                        Method existingGetKey = AttributeModifier.class.getMethod("getKey");
                        Object existingKey = existingGetKey.invoke(existing);
                        return existingKey instanceof NamespacedKey && ((NamespacedKey) existingKey).equals(modKey);
                    } catch (Exception e) {
                        return false;
                    }
                });
            }
        } catch (Exception e) {
            // Fallback to UUID
            UUID legacyUUID = toUUID(key);
            alreadyExists = toughnessAttr.getModifiers().stream()
                    .anyMatch(mod -> mod.getUniqueId().equals(legacyUUID));
        }

        if (!alreadyExists) {
            toughnessAttr.addModifier(modifier);
        }
    }


    public void removeArmorModifier(Player player, NamespacedKey key) {
        AttributeInstance armorAttr = getArmorAttribute(player);
        if (armorAttr == null) return;

        for (AttributeModifier modifier : armorAttr.getModifiers()) {
            try {
                Method getKeyMethod = AttributeModifier.class.getMethod("getKey");
                Object result = getKeyMethod.invoke(modifier);

                if (result instanceof NamespacedKey modKey && modKey.equals(key)) {
                    armorAttr.removeModifier(modifier);
                    return;
                }
            } catch (Exception e) {
                // Fallback to UUID for 1.18
                UUID legacyUUID = toUUID(key);
                if (modifier.getUniqueId().equals(legacyUUID)) {
                    armorAttr.removeModifier(modifier);
                    return;
                }
            }
        }
    }


    public void removeToughnessModifier(Player player, NamespacedKey key) {
        AttributeInstance toughnessAttr = getToughnessAttribute(player);
        if (toughnessAttr == null) return;

        for (AttributeModifier modifier : toughnessAttr.getModifiers()) {
            try {
                Method getKeyMethod = AttributeModifier.class.getMethod("getKey");
                Object result = getKeyMethod.invoke(modifier);

                if (result instanceof NamespacedKey modKey && modKey.equals(key)) {
                    toughnessAttr.removeModifier(modifier);
                    return;
                }
            } catch (Exception e) {
                UUID legacyUUID = toUUID(key);
                if (modifier.getUniqueId().equals(legacyUUID)) {
                    toughnessAttr.removeModifier(modifier);
                    return;
                }
            }
        }
    }



    private AttributeModifier createModifier(NamespacedKey key, double amount, String name, AttributeModifier.Operation op) {
        UUID uuid = toUUID(key);

        try {
            // Try EquipmentSlotGroup for newer versions
            Class<?> slotGroupClass = Class.forName("org.bukkit.inventory.EquipmentSlotGroup");
            Object chestSlotGroup = slotGroupClass.getField("CHEST").get(null);

            return AttributeModifier.class
                    .getConstructor(NamespacedKey.class, double.class, AttributeModifier.Operation.class, slotGroupClass)
                    .newInstance(key, amount, op, chestSlotGroup);

        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            // Fallback for older versions without EquipmentSlotGroup
            try {
                return AttributeModifier.class
                        .getConstructor(UUID.class, String.class, double.class, AttributeModifier.Operation.class)
                        .newInstance(uuid, name, amount, op);
            } catch (Exception ex) {
                plugin.getLogger().severe("Failed to create AttributeModifier: " + ex.getMessage() + "Report this to the plugin author!");
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create AttributeModifier: " + e.getMessage() + "Report this to the plugin author!");
            return null;
        }
    }

    private UUID toUUID(NamespacedKey key) {
        String combined = key.getNamespace() + ":" + key.getKey();
        return UUID.nameUUIDFromBytes(combined.getBytes(StandardCharsets.UTF_8));
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
    }

    private String getCapitalizedName(String name) {
        String[] words = name.toLowerCase().replace("_", " ").split(" ");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            capitalized.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return capitalized.toString().trim();
    }

    private String getStandardEnchantmentKey(Enchantment enchant) {
        String key = enchant.getKey().getKey();

        // Fallback for older versions (1.20 and earlier)
        if (ServerVersion.getCurrent().equals(ServerVersion.V_1_18) || ServerVersion.getCurrent().equals(ServerVersion.V_1_19)
                || ServerVersion.getCurrent().equals(ServerVersion.V_1_20))
        {
            return switch (key) {
                case "protection" -> "protection_environmental";
                case "fire_protection" -> "protection_fire";
                case "blast_protection" -> "protection_explosions";
                case "projectile_protection" -> "protection_projectile";
                default -> key;
            };
        }

        return key;
    }
}
