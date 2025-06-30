package org.bruno.elytraEssentials.handlers;

import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bruno.elytraEssentials.utils.ServerVersion;
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
import java.util.*;

public class EffectsHandler {
    private final ElytraEssentials plugin;

    private final Map<String, ElytraEffect> effectsRegistry = new HashMap<>();

    public EffectsHandler(ElytraEssentials plugin, FileConfiguration fileConfiguration) {
        this.plugin = plugin;

        registerEffects();

        loadEffectsConfig(fileConfiguration);
    }

    public boolean handlePurchase(Player player, String effectKey, String effectPermission) {
        try {
            List<String> ownedEffects = plugin.getDatabaseHandler().GetOwnedEffectKeys(player.getUniqueId());

            if (PermissionsHelper.hasElytraEffectsPermission(player) || player.hasPermission(effectPermission) || ownedEffects.contains(effectKey)) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getEffectGuiOwned());
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
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getPurchaseFailedMoney());
                return false;
            }

            economy.withdrawPlayer(player, price);
            plugin.getDatabaseHandler().AddOwnedEffect(player.getUniqueId(), effectKey);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);

            String message = plugin.getMessagesHandlerInstance().getPurchaseSuccessful().replace("{0}", ColorHelper.ParseColoredString(effect.getName()));
            plugin.getMessagesHelper().sendPlayerMessage(player, message);
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
            if (oldEffect != null)
                plugin.getDatabaseHandler().UpdateOwnedEffect(player.getUniqueId(), oldEffectName, false);

            ElytraEffect selectedEffect = effectsRegistry.getOrDefault(effectKey, null);
            if (selectedEffect == null){
                plugin.getLogger().severe("An error occurred while trying to get the selected effect!" );
                return false;
            }

            if (selectedEffect.getIsActive())
                return false;

            plugin.getDatabaseHandler().UpdateOwnedEffect(player.getUniqueId(), effectKey, true);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);

            String message = plugin.getMessagesHandlerInstance().getEffectSelected().replace("{0}", ColorHelper.ParseColoredString(selectedEffect.getName()));
            plugin.getMessagesHelper().sendPlayerMessage(player, message);

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

    public String getActiveEffect(UUID playerUuid) {
        try {
            return plugin.getDatabaseHandler().getPlayerActiveEffect(playerUuid);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles the deactivation of a player's currently active effect.
     * @param player The player deactivating the effect.
     * @param effectKey The key of the effect to deactivate.
     * @return true if the effect was successfully deactivated, false otherwise.
     */
    public boolean handleDeselection(Player player, String effectKey) {
        try {
            String activeEffect = plugin.getDatabaseHandler().getPlayerActiveEffect(player.getUniqueId());
            if (activeEffect == null)
                return false;

            if (!effectKey.equals(activeEffect))
                return false;

            // Check if the effect they are right-clicking is actually the one that's active.
            plugin.getDatabaseHandler().UpdateOwnedEffect(player.getUniqueId(), effectKey, false);

            ElytraEffect elytraEffect = getEffectsRegistry().get(effectKey);
            if (elytraEffect != null)
                elytraEffect.setIsActive(false);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);

            String message = plugin.getMessagesHandlerInstance().getEffectDeselected().replace("{0}", ColorHelper.ParseColoredString(elytraEffect.getName()));
            plugin.getMessagesHelper().sendPlayerMessage(player, message);
            plugin.getElytraFlightListener().UpdateEffect(null);
            return true;
        }
        catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while updating your effect.");
            e.printStackTrace();
            return false;
        }
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
            lore.add(ChatColor.translateAlternateColorCodes('&', plugin.getMessagesHandlerInstance().getEffectGuiOwned()));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', plugin.getMessagesHandlerInstance().getEffectGuiPurchase()));
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
            lore.add(ChatColor.translateAlternateColorCodes('&', plugin.getMessagesHandlerInstance().getEffectGuiDeselect()));

            if (meta != null) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        else{
            lore.add(ChatColor.translateAlternateColorCodes('&', plugin.getMessagesHandlerInstance().getEffectGuiSelect()));
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
        // Your existing effects that work on all versions
        effectsRegistry.put("FIRE_TRAIL", new ElytraEffect(
                "Fire Trail", Material.CAMPFIRE, Particle.FLAME,
                List.of("§7Leave a fiery trail!"), 1000, "elytraessentials.effect.fire"
        ));
        effectsRegistry.put("ICE_SHARDS", new ElytraEffect(
                "Ice Shards", Material.ICE, Particle.SNOWFLAKE,
                List.of("§7Shards of ice behind you!"), 1500, "elytraessentials.effect.ice"
        ));
        effectsRegistry.put("INKY_VOID", new ElytraEffect(
                "Inky Void", Material.INK_SAC, Particle.SQUID_INK,
                List.of("§7Soar with a trail of darkness."), 1750, "elytraessentials.effect.void"
        ));
        effectsRegistry.put("HEART_TRAIL", new ElytraEffect(
                "Heart Trail", Material.POPPY, Particle.HEART,
                List.of("§7Spread love wherever you fly!"), 1250, "elytraessentials.effect.heart"
        ));

        effectsRegistry.put("WITCHS_BREW", new ElytraEffect(
                "Witch's Brew", Material.BREWING_STAND, Particle.WITCH,
                List.of("§7A swirling, magical concoction follows you."), 2200, "elytraessentials.effect.witch"
        ));

        effectsRegistry.put("SOUL_FIRE", new ElytraEffect(
                "Soul Fire", Material.SOUL_LANTERN, Particle.SOUL_FIRE_FLAME,
                List.of("§7A trail of eerie, blue soul fire."), 1800, "elytraessentials.effect.soulfire"
        ));

        effectsRegistry.put("MUSICAL_FLIGHT", new ElytraEffect(
                "Musical Flight", Material.JUKEBOX, Particle.NOTE,
                List.of("§7Leave a trail of musical notes as you fly."), 1300, "elytraessentials.effect.note"
        ));

        effectsRegistry.put("CLOUD_SURFER", new ElytraEffect(
                "Cloud Surfer", Material.WHITE_WOOL, Particle.CLOUD,
                List.of("§7Fly on your own personal cloud."), 800, "elytraessentials.effect.cloud"
        ));

        effectsRegistry.put("SLIME_TRAIL", new ElytraEffect(
                "Slime Trail", Material.SLIME_BALL, Particle.ITEM_SLIME,
                List.of("§7Leave a gooey, green trail behind."), 900, "elytraessentials.effect.slime"
        ));

        effectsRegistry.put("CRITICAL_AURA", new ElytraEffect(
                "Critical Aura", Material.DIAMOND_SWORD, Particle.CRIT,
                List.of("§7Fly with the power of a critical hit!"), 2100, "elytraessentials.effect.crit"
        ));

        effectsRegistry.put("ENDERS_WAKE", new ElytraEffect(
                "Ender's Wake", Material.ENDER_EYE, Particle.PORTAL,
                List.of("§7A swirling vortex of the void follows you."), 2800, "elytraessentials.effect.portal"
        ));

        effectsRegistry.put("SPORE_BLOOM", new ElytraEffect(
                "Spore Bloom", Material.SPORE_BLOSSOM, Particle.SPORE_BLOSSOM_AIR,
                List.of("§7A trail of beautiful, floating spores."), 2800, "elytraessentials.effect.spore"
        ));

        effectsRegistry.put("BUBBLE_STREAM", new ElytraEffect(
                "Bubble Stream", Material.GLASS_BOTTLE, Particle.BUBBLE_POP,
                List.of("§7A stream of popping bubbles."), 950, "elytraessentials.effect.bubble"
        ));

        effectsRegistry.put("DRAGON_BREATH", new ElytraEffect(
                "Dragon Breath", Material.DRAGON_EGG, Particle.DRAGON_BREATH,
                List.of("§7The spark of dragon breath."), 2400, "elytraessentials.effect.dragon"
        ));

        effectsRegistry.put("DOLPHINS_GRACE", new ElytraEffect(
                "Dolphin's Grace", Material.HEART_OF_THE_SEA, Particle.DOLPHIN,
                List.of("§7Swim through the air with a dolphin's help."), 3200, "elytraessentials.effect.dolphin"
        ));

        effectsRegistry.put("DAMAGE_FLASH", new ElytraEffect(
                "Damage Flash", Material.REDSTONE, Particle.DAMAGE_INDICATOR,
                List.of("§7A flash of red damage particles."), 1650, "elytraessentials.effect.damage"
        ));

        effectsRegistry.put("WAXED_WINGS", new ElytraEffect(
                "Waxed Wings", Material.HONEYCOMB, Particle.WAX_ON,
                List.of("§7A trail of waxy particles."), 1400, "elytraessentials.effect.wax"
        ));

        registerVersionDependentEffect("EXPLOSIVE_TRAIL", "Explosive Trail", Material.TNT,
                Map.of(
                        ServerVersion.V_1_18, "EXPLOSION_NORMAL",
                        ServerVersion.V_1_19, "EXPLOSION_NORMAL",
                        ServerVersion.V_1_20, "EXPLOSION_NORMAL",
                        ServerVersion.V_1_21, "EXPLOSION"
                ),
                List.of("§7A trail of small explosions."), 1100, "elytraessentials.effect.explosion");

        registerVersionDependentEffect("SMOKE_SCREEN", "Smoke Screen", Material.BLACK_DYE,
                Map.of(
                        ServerVersion.V_1_18, "SMOKE_LARGE",
                        ServerVersion.V_1_19, "SMOKE_LARGE",
                        ServerVersion.V_1_20, "SMOKE_LARGE",
                        ServerVersion.V_1_21, "LARGE_SMOKE"
                ),
                List.of("§7Create a screen of thick smoke."), 1100, "elytraessentials.effect.smoke");

        registerVersionDependentEffect("TOTEM_BLESSING", "Totem's Blessing", Material.TOTEM_OF_UNDYING,
                Map.of(
                        ServerVersion.V_1_18, "TOTEM",
                        ServerVersion.V_1_19, "TOTEM",
                        ServerVersion.V_1_20, "TOTEM",
                        ServerVersion.V_1_21, "TOTEM_OF_UNDYING"
                ),
                List.of("§7Fly with the blessing of immortality!"), 3500, "elytraessentials.effect.totem");


        registerVersionDependentEffect("LAVA_DRIP", "Lava Trail", Material.LAVA_BUCKET,
                Map.of(
                        ServerVersion.V_1_18, "DRIP_LAVA",
                        ServerVersion.V_1_19, "DRIP_LAVA",
                        ServerVersion.V_1_20, "DRIP_LAVA",
                        ServerVersion.V_1_21, "DRIPPING_LAVA"
                ),
                List.of("§7Drips of hot magma follow you."), 1900, "elytraessentials.effect.lavadrip");

        registerVersionDependentEffect("WATER_TRAIL", "Water Trail", Material.WATER_BUCKET,
                Map.of(
                        ServerVersion.V_1_18, "DRIP_WATER",
                        ServerVersion.V_1_19, "DRIP_WATER",
                        ServerVersion.V_1_20, "DRIP_WATER",
                        ServerVersion.V_1_21, "DRIPPING_WATER"
                ),
                List.of("§7Trails of water follow you!"), 2000, "elytraessentials.effect.water");

        registerVersionDependentEffect("ARCANE_TRAIL", "Arcane Trail", Material.ENCHANTING_TABLE,
                Map.of(
                        ServerVersion.V_1_18, "ENCHANTMENT_TABLE",
                        ServerVersion.V_1_19, "ENCHANTMENT_TABLE",
                        ServerVersion.V_1_20, "ENCHANTMENT_TABLE",
                        ServerVersion.V_1_21, "ENCHANTMENT_TABLE"
                ),
                List.of("§7Leave a trail of mystical runes."), 2500, "elytraessentials.effect.arcane");

        registerVersionDependentEffect("EMERALD_SPARK", "Emerald Spark", Material.EMERALD,
                Map.of(
                        ServerVersion.V_1_18, "VILLAGER_HAPPY",
                        ServerVersion.V_1_19, "VILLAGER_HAPPY",
                        ServerVersion.V_1_20, "VILLAGER_HAPPY",
                        ServerVersion.V_1_21, "HAPPY_VILLAGER" // Renamed in 1.21
                ),
                List.of("§7Show off with a glittering green trail."), 3000, "elytraessentials.effect.emerald");


        if (ServerVersion.getCurrent() == ServerVersion.V_1_20 || ServerVersion.getCurrent() == ServerVersion.V_1_21) {
            effectsRegistry.put("CHERRY_BLOSSOM", new ElytraEffect(
                    "Waxed Cherry Blossom", Material.CHERRY_LEAVES, Particle.CHERRY_LEAVES,
                    List.of("§7A beautiful trail of cherry petals."), 1550, "elytraessentials.effect.cherry"
            ));
        }
    }

    private void registerVersionDependentEffect(String key, String name, Material material,
                                                Map<ServerVersion, String> particleNames,
                                                List<String> lore, int price, String permission) {

        ServerVersion currentVersion = plugin.getServerVersion();
        if (currentVersion == null || currentVersion == ServerVersion.UNKNOWN) {
            plugin.getLogger().warning("Could not register effect '" + name + "' due to an unknown server version.");
            return;
        }

        String particleNameToUse = particleNames.getOrDefault(currentVersion, particleNames.get(ServerVersion.V_1_21));

        if (particleNameToUse == null) {
            plugin.getLogger().warning("Could not register effect '" + name + "'. No valid particle name was defined for this version.");
            return;
        }

        try {
            Particle particle = Particle.valueOf(particleNameToUse.toUpperCase());
            effectsRegistry.put(key, new ElytraEffect(name, material, particle, lore, price, permission));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Could not register effect '" + name + "'. Particle '" + particleNameToUse + "' is not valid on this server version.");
        }
    }

    public Map<String, ElytraEffect> getEffectsRegistry(){
        return effectsRegistry;
    }
}
