package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled())
            return;

        if (!(e.getWhoClicked() instanceof Player player)) return;

        // Direct click in the chestplate slot
        if (e.getSlotType() == InventoryType.SlotType.ARMOR && e.getSlot() == Constants.Inventory.CHESTPLATE_SLOT) {
            scheduleArmorCheck(player);
        }

        // Shift-clicking an item from the main inventory
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && isArmoredElytra(e.getCurrentItem())  ) {
            scheduleArmorCheck(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled())
            return;

        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (isArmoredElytra(e.getItem())) {
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

        int armorPoints = getArmorPoints(armorType);
        int armorToughness = getArmorToughness(armorType);

        AttributeInstance armorAttr = plugin.getArmoredElytraHelper().getArmorAttribute(player);
        AttributeInstance toughnessAttr = plugin.getArmoredElytraHelper().getToughnessAttribute(player);

        if (armorAttr == null || toughnessAttr == null) return;

        if (armorPoints > 0){
            plugin.getArmoredElytraHelper().setArmorModifier(player, armoredElytraKey, armorPoints);
        }

        if (armorToughness > 0) {
            plugin.getArmoredElytraHelper().setToughnessModifier(player, toughnessElytraKey, armorToughness);
        }
    }

    private void removeArmorAttributes(Player player) {
        AttributeInstance armorAttr = plugin.getArmoredElytraHelper().getArmorAttribute(player);
        AttributeInstance toughnessAttr = plugin.getArmoredElytraHelper().getToughnessAttribute(player);

        if (armorAttr != null) {
            plugin.getArmoredElytraHelper().removeArmorModifier(player, armoredElytraKey);
        }

        if (toughnessAttr != null) {
            plugin.getArmoredElytraHelper().removeToughnessModifier(player, toughnessElytraKey);
        }
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
    }
}