package org.bruno.elytraEssentials.handlers;

import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EffectsHandler {
    private final ElytraEssentials plugin;

    private final Map<String, ElytraEffect> effectsRegistry = new HashMap<>();

    public EffectsHandler(ElytraEssentials plugin, FileConfiguration fileConfiguration) {
        this.plugin = plugin;

        //  register effects
        registerEffects();

        //  update effects from shop.yml
        loadEffectsConfig(fileConfiguration);
    }

    public boolean handlePurchase(Player player, String effectKey, String effectPermission) {
        try {
            List<String> ownedEffects = plugin.getDatabaseHandler().GetOwnedEffectKeys(player.getUniqueId());

            if (PermissionsHelper.hasElytraEffectsPermission(player) || player.hasPermission(effectPermission) || ownedEffects.contains(effectKey)) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
                player.sendMessage(ChatColor.YELLOW + "You already own this effect!");
                return false;
            }

            ElytraEffect effect = effectsRegistry.getOrDefault(effectKey, null);
            if (effect == null)
            {
                plugin.getLogger().severe("An error occurred while trying to get the wanted effect to buy!" );
                return false;
            }

            double price = effect.getPrice();
            Economy economy = plugin.getEconomy();

            if (!economy.has(player, price)) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
                player.sendMessage(ChatColor.RED + "You do not have enough money to purchase this effect.");
                return false;
            }

            economy.withdrawPlayer(player, price);
            plugin.getDatabaseHandler().AddOwnedEffect(player.getUniqueId(), effectKey);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);
            player.sendMessage(ChatColor.GRAY + "You have purchased the " + ColorHelper.ParseColoredString(effect.getName()) + ChatColor.GRAY + " effect!");
            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
            player.sendMessage(ChatColor.RED + "An error occurred while processing your purchase.");
        }
        return false;
    }

    public boolean handleSelection(Player player, String effectKey) {
        try {
            //  Make sure the other effect that may be selected gets deactivated so we don't get multiple active
            String oldEffectName = plugin.getDatabaseHandler().getPlayerActiveEffect(player.getUniqueId());
            ElytraEffect oldEffect = effectsRegistry.getOrDefault(oldEffectName, null);
            if (oldEffect != null){
                plugin.getDatabaseHandler().UpdateOwnedEffect(player.getUniqueId(), oldEffectName, false);
            }

            ElytraEffect selectedEffect = effectsRegistry.getOrDefault(effectKey, null);
            if (selectedEffect == null){
                plugin.getLogger().severe("An error occurred while trying to get the selected effect!" );
                return false;
            }

            if (selectedEffect.getIsActive())
                plugin.getDatabaseHandler().UpdateOwnedEffect(player.getUniqueId(), effectKey, false);
            else
                plugin.getDatabaseHandler().UpdateOwnedEffect(player.getUniqueId(), effectKey, true);


            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);
            player.sendMessage(ChatColor.GRAY + "You have selected " + ColorHelper.ParseColoredString(selectedEffect.getName()) + ChatColor.GRAY + " effect!");

            plugin.getElytraFlightListener().UpdateEffect(selectedEffect);
            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
            player.sendMessage(ChatColor.RED + "An error occurred while trying to activate the effect!" );
        }
        return false;
    }

    public ItemStack createShopItem(String effectKey, ElytraEffect effect, Player player) {
        ItemStack item = new ItemStack(effect.getDisplayMaterial());
        ItemMeta meta = item.getItemMeta();

        List<String> ownedEffects;
        try {
            ownedEffects = plugin.getDatabaseHandler().GetOwnedEffectKeys(player.getUniqueId());
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Price: "  + ChatColor.YELLOW  + "$" + effect.getPrice());
        lore.add("");
        for (String loreLine : effect.getLore()) {
            lore.add(ColorHelper.ParseColoredString(loreLine));
        }
        lore.add("");

        if (player.hasPermission(effect.getPermission()) || ownedEffects.contains(effectKey)) {
            lore.add(ChatColor.RED + "§lYou already own this effect!");
        } else {
            lore.add(ChatColor.GREEN + "§lClick to purchase!");
        }

        if (meta != null) {
            meta.setDisplayName(ColorHelper.ParseColoredString(effect.getName()));
            meta.setLore(lore);

            // Store the ID in the PersistentDataContainer
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "effect_key"),
                    PersistentDataType.STRING,
                    effectKey
            );

            // Store the permission in the PersistentDataContainer
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "effect_permission"),
                    PersistentDataType.STRING,
                    effect.getPermission()
            );
        }

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createOwnedItem(String effectKey, ElytraEffect effect) {
        ItemStack item = new ItemStack(effect.getDisplayMaterial());
        ItemMeta meta = item.getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String loreLine : effect.getLore()) {
            lore.add(ColorHelper.ParseColoredString(loreLine));
        }
        lore.add("");
        if (effect.getIsActive()){
            lore.add(ChatColor.RED + "§lAlready selected!");

            // Add a visual enchantment effect
            if (meta != null) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        else{
            lore.add(ChatColor.GREEN + "§lSelect this effect!");
        }

        if (meta != null) {
            meta.setDisplayName(ColorHelper.ParseColoredString(effect.getName()));
            meta.setLore(lore);

            // Store the key in the PersistentDataContainer
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "effect_key"),
                PersistentDataType.STRING,
                effectKey
            );
        }

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createEmptyItemStack() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null){
            meta.setDisplayName("§c§lPurchase Effects!");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.WHITE + "You don't own any elytra effect");
            lore.add(ChatColor.WHITE + "Visit the shop for more info");
            lore.add("");
            lore.add(ChatColor.GRAY + "/ee shop");
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    public void spawnParticleTrail(Player player, ElytraEffect effect) {
        // Get the player's location
        Vector direction = player.getLocation().getDirection().normalize();
        Vector offset = direction.multiply(-1); // Offset behind the player
        Vector particleLocation = player.getLocation().toVector().add(offset).toLocation(player.getWorld()).toVector();

        // Spawn particles at the adjusted location
        player.getWorld().spawnParticle(effect.getParticle(),
                particleLocation.toLocation(player.getWorld()),
                10, // Number of particles
                0.1, // X offset
                0.1, // Y offset
                0.1, // Z offset
                0.05); // Speed multiplier
    }

    private void loadEffectsConfig(FileConfiguration fileConfiguration) {
        for (String key : fileConfiguration.getConfigurationSection("shop").getKeys(false)) {

            ElytraEffect effect = effectsRegistry.get(key);

            if (effect != null) {
                effect.setPrice(fileConfiguration.getDouble("shop." + key + ".price"));
                effect.setPermission(fileConfiguration.getString("shop." + key + ".permission"));

                String effectName = fileConfiguration.getString("shop." + key + ".name");
                if (effectName != null)
                    effect.setName(ColorHelper.ParseColoredString(effectName));

                // Handle lore as a list
                List<String> loreLines = fileConfiguration.getStringList("shop." + key + ".lore");
                List<String> translatedLore = new ArrayList<>();
                for (String line : loreLines) {
                    translatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                effect.setLore(translatedLore); // Assuming setLore accepts a List<String>
            }

            effectsRegistry.put(key, effect);
        }
    }

    private void registerEffects() {
        effectsRegistry.put("FIRE_TRAIL", new ElytraEffect(
                "Fire Trail",
                Material.CAMPFIRE,
                Particle.FLAME,
                List.of("&7Leave a fiery trail!"),
                1000,
                "elytraessentials.effect.fire"
        ));
        effectsRegistry.put("WATER_TRAIL", new ElytraEffect(
                "Water Trail",
                Material.WATER_BUCKET,
                Particle.DRIPPING_WATER,
                List.of("&7Trails of water follow you!"),
                2000,
                "elytraessentials.effect.water"
        ));
        effectsRegistry.put("ICE_SHARDS", new ElytraEffect(
                "Ice Shards",
                Material.ICE,
                Particle.SNOWFLAKE,
                List.of("&7Shards of ice behind you!"),
                1500,
                "elytraessentials.effect.ice"
        ));

        effectsRegistry.put("ARCANE_TRAIL", new ElytraEffect(
                "Arcane Trail",
                Material.ENCHANTING_TABLE,
                Particle.ENCHANT,
                List.of("§7Leave a trail of mystical runes."),
                2500,
                "elytraessentials.effect.arcane"
        ));

        effectsRegistry.put("INKY_VOID", new ElytraEffect(
                "Inky Void",
                Material.INK_SAC,
                Particle.SQUID_INK,
                List.of("§7Soar with a trail of darkness."),
                1750,
                "elytraessentials.effect.void"
        ));

        effectsRegistry.put("HEART_TRAIL", new ElytraEffect(
                "Heart Trail",
                Material.POPPY,
                Particle.HEART,
                List.of("§7Spread love wherever you fly!"),
                1250,
                "elytraessentials.effect.heart"
        ));

        effectsRegistry.put("EMERALD_SPARK", new ElytraEffect(
                "Emerald Spark",
                Material.EMERALD,
                Particle.HAPPY_VILLAGER,
                List.of("§7Show off with a glittering green trail."),
                3000,
                "elytraessentials.effect.emerald"
        ));
    }

    public Map<String, ElytraEffect> getEffectsRegistry(){
        return effectsRegistry;
    }
}
