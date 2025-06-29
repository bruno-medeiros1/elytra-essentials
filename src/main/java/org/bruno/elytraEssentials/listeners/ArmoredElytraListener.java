package org.bruno.elytraEssentials.listeners;

import com.github.jewishbanana.playerarmorchangeevent.PlayerArmorChangeEvent;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.attribute.Attribute;

import java.util.Objects;

//  Apply or remove the player's armor stat buffs.
public class ArmoredElytraListener implements Listener {

    private final ElytraEssentials plugin;

    private final NamespacedKey armoredElytraKey;
    private final NamespacedKey toughnessElytraKey;
    private final NamespacedKey materialKey;
    private final NamespacedKey durabilityKey;

    public ArmoredElytraListener(ElytraEssentials plugin) {
        this.plugin = plugin;

        this.armoredElytraKey = new NamespacedKey(plugin, Constants.NBT.ARMORED_ELYTRA_TAG);
        this.toughnessElytraKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG);
        this.materialKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_MATERIAL_TAG);
        this.durabilityKey = new NamespacedKey(plugin, Constants.NBT.ARMOR_DURABILITY_TAG);
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent e) {
        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled())
            return;

        if (e.getSlot() == EquipmentSlot.CHEST) {
            scheduleArmorCheck(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isArmoredElytra(event.getEntity().getInventory().getChestplate())) {
            removeArmorAttributes(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        scheduleArmorCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        if (isArmoredElytra(event.getBrokenItem())) {
            removeArmorAttributes(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDispense(BlockDispenseArmorEvent event) {
        if (event.getTargetEntity() instanceof Player player && isArmoredElytra(event.getItem())) {
            scheduleArmorCheck(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleArmorCheck(event.getPlayer());
    }

    private void scheduleArmorCheck(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> checkArmor(player));
    }

    /**
     * Checks the player's chestplate slot and applies or removes Armored Elytra attributes.
     * @param player The player to check.
     */
    private void checkArmor(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();

        removeArmorAttributes(player);

        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled()) {
            return;
        }

        if (isArmoredElytra(chestplate)){
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

        double armorPoints = getArmorPoints(armorType);
        double armorToughness = getArmorToughness(armorType);

        AttributeInstance armorAttr = player.getAttribute(Attribute.ARMOR);
        AttributeInstance toughnessAttr = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
        if (armorAttr == null || toughnessAttr == null) return;

        if (armorPoints > 0){
            AttributeModifier armorMod = new AttributeModifier(armoredElytraKey, armorPoints, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
            armorAttr.addModifier(armorMod);
        }

        if (armorToughness > 0) {
            AttributeModifier toughnessMod = new AttributeModifier(toughnessElytraKey, armorToughness, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
            toughnessAttr.addModifier(toughnessMod);
        }
    }

    private void removeArmorAttributes(Player player) {
        AttributeInstance armorAttr = player.getAttribute(Attribute.ARMOR);
        AttributeInstance toughnessAttr = player.getAttribute(Attribute.ARMOR_TOUGHNESS);

        if (armorAttr != null) {
            for (AttributeModifier modifier : armorAttr.getModifiers()) {
                if (modifier.getKey().equals(armoredElytraKey)) {
                    armorAttr.removeModifier(modifier);
                    break;
                }
            }
        }

        if (toughnessAttr != null) {
            for (AttributeModifier modifier : toughnessAttr.getModifiers()) {
                if (modifier.getKey().equals(toughnessElytraKey)) {
                    toughnessAttr.removeModifier(modifier);
                    break;
                }
            }
        }
    }


    private boolean isArmoredElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(armoredElytraKey, PersistentDataType.BOOLEAN);
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
    }
}