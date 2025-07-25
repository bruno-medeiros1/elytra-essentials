package org.bruno.elytraEssentials.handlers;

import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.Constants;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class EffectsHandler {
    private final ElytraEssentials plugin;
    private final FoliaHelper foliaHelper;
    private final DatabaseHandler databaseHandler;
    private final MessagesHelper messagesHelper;
    private final ServerVersion serverVersion;
    private final Economy economy;
    private final TpsHandler tpsHandler;
    private final MessagesHandler messagesHandler;
    private final Logger logger;

    private final Map<String, ElytraEffect> effectsRegistry = new HashMap<>();

    private final Map<UUID, String> activePlayerEffects = new HashMap<>();

    public EffectsHandler(ElytraEssentials plugin, FileConfiguration fileConfiguration, FoliaHelper foliaHelper, DatabaseHandler databaseHandler, MessagesHelper messagesHelper,
                          ServerVersion serverVersion, Economy economy, TpsHandler tpsHandler, MessagesHandler messagesHandler, Logger logger) {
        this.plugin = plugin;
        this.foliaHelper = foliaHelper;
        this.databaseHandler = databaseHandler;
        this.messagesHelper = messagesHelper;
        this.serverVersion = serverVersion;
        this.economy = economy;
        this.tpsHandler = tpsHandler;
        this.messagesHandler = messagesHandler;
        this.logger = logger;

        registerEffects();
        loadEffectsConfig(fileConfiguration);
    }

    public boolean handlePurchase(Player player, String effectKey, String effectPermission) {
        try {
            List<String> ownedEffects = databaseHandler.getOwnedEffectKeys(player.getUniqueId());

            if (PermissionsHelper.hasAllEffectsPermission(player) || player.hasPermission(effectPermission) || ownedEffects.contains(effectKey)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
                messagesHelper.sendPlayerMessage(player, messagesHandler.getEffectGuiOwned());
                return false;
            }

            ElytraEffect effect = effectsRegistry.get(effectKey);
            if (effect == null) return false;

            if (economy != null && !economy.has(player, effect.getPrice())) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
                messagesHelper.sendPlayerMessage(player, messagesHandler.getNotEnoughMoney());
                return false;
            }

            if (economy != null) {
                economy.withdrawPlayer(player, effect.getPrice());
            }
            databaseHandler.addOwnedEffect(player.getUniqueId(), effectKey);

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);
            String message = messagesHandler.getPurchaseSuccessful().replace("{0}", ColorHelper.parse(effect.getName()));
            messagesHelper.sendPlayerMessage(player, message);
            return true;
        } catch (SQLException e) {
            handleSqlException(e, player, "&cAn error occurred while processing your purchase.");
            return false;
        }
    }

    public boolean handleSelection(Player player, String effectKey) {
        try {
            String currentActive = getActiveEffect(player.getUniqueId());
            if (effectKey.equals(currentActive)) return false; // Already active

            // Deactivate the old effect first
            if (currentActive != null) {
                databaseHandler.updateOwnedEffect(player.getUniqueId(), currentActive, false);
            }

            // Activate the new effect
            databaseHandler.updateOwnedEffect(player.getUniqueId(), effectKey, true);
            setActiveEffect(player.getUniqueId(), effectKey); // Update cache

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);
            String message = messagesHandler.getEffectSelected().replace("{0}", ColorHelper.parse(effectsRegistry.get(effectKey).getName()));
            messagesHelper.sendPlayerMessage(player, message);
            return true;
        } catch (SQLException e) {
            handleSqlException(e, player, "&cAn error occurred while trying to activate the effect!");
            return false;
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
            String activeEffect = getActiveEffect(player.getUniqueId());
            if (!effectKey.equals(activeEffect)) return false;

            databaseHandler.updateOwnedEffect(player.getUniqueId(), effectKey, false);
            setActiveEffect(player.getUniqueId(), null); // Clear from cache

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);
            String message = messagesHandler.getEffectDeselected().replace("{0}", ColorHelper.parse(effectsRegistry.get(effectKey).getName()));
            messagesHelper.sendPlayerMessage(player, message);
            return true;
        } catch (SQLException e) {
            handleSqlException(e, player, "&cAn error occurred while updating your effect.");
            return false;
        }
    }

    public ItemStack createShopItem(String effectKey, ElytraEffect effect, Player player) {
        ItemStack item = new ItemStack(effect.getDisplayMaterial());
        ItemMeta meta = item.getItemMeta();

        List<String> ownedEffects;
        try {
            ownedEffects = databaseHandler.getOwnedEffectKeys(player.getUniqueId());
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        List<String> lore = new ArrayList<>();
        lore.add("§6Price: " + "§e$" + effect.getPrice());
        lore.add("");
        for (String loreLine : effect.getLore()) {
            lore.add(ColorHelper.parse(loreLine));
        }
        lore.add("");

        if (player.hasPermission(effect.getPermission()) || ownedEffects.contains(effectKey)) {
            lore.add(ColorHelper.parse(messagesHandler.getEffectGuiOwned()));
        } else {
            lore.add(ColorHelper.parse(messagesHandler.getEffectGuiPurchase()));
        }

        if (meta != null) {
            meta.setDisplayName(ColorHelper.parse(effect.getName()));
            meta.setLore(lore);

            // Store the ID in the PersistentDataContainer
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, Constants.NBT.EFFECT_KEY),
                    PersistentDataType.STRING,
                    effectKey
            );

            // Store the permission in the PersistentDataContainer
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, Constants.NBT.EFFECT_PERMISSION_KEY),
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
            lore.add(ColorHelper.parse(loreLine));
        }
        lore.add("");
        if (effect.getIsActive()){
            lore.add(ColorHelper.parse(messagesHandler.getEffectGuiDeselect()));

            if (meta != null) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        else{
            lore.add(ColorHelper.parse(messagesHandler.getEffectGuiSelect()));
        }

        if (meta != null) {
            meta.setDisplayName(ColorHelper.parse(effect.getName()));
            meta.setLore(lore);

            // Store the key in the PersistentDataContainer
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, Constants.NBT.EFFECT_KEY),
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
            lore.add("§fYou don't own any elytra effect");
            lore.add("§fVisit the shop for more info");
            lore.add("");
            lore.add("§7/ee shop");
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    public void spawnParticleTrail(Player player) {
        String activeEffectKey = getActiveEffect(player.getUniqueId());
        if (activeEffectKey == null) return;

        ElytraEffect effect = effectsRegistry.get(activeEffectKey);
        if (effect == null || tpsHandler.isLagProtectionActive()) return;

        Vector direction = player.getLocation().getDirection().normalize();
        Vector offset = direction.multiply(-1);
        Vector particleLocation = player.getLocation().toVector().add(offset);

        player.getWorld().spawnParticle(effect.getParticle(), particleLocation.toLocation(player.getWorld()), 10, 0.1, 0.1, 0.1, 0.05);
    }

    public String getActiveEffect(UUID playerUuid) {
        return activePlayerEffects.get(playerUuid);
    }

    public void setActiveEffect(UUID playerUuid, String effectKey) {
        if (effectKey == null) {
            activePlayerEffects.remove(playerUuid);
        } else {
            activePlayerEffects.put(playerUuid, effectKey);
        }
    }

    public void loadPlayerActiveEffect(Player player) {
        foliaHelper.runAsyncTask(() -> {
            try {
                String activeEffect = databaseHandler.getPlayerActiveEffect(player.getUniqueId());
                if (activeEffect != null) {
                    setActiveEffect(player.getUniqueId(), activeEffect);
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Could not load active effect for " + player.getName(), e);
            }
        });
    }

    public void clearPlayerActiveEffect(Player player) {
        activePlayerEffects.remove(player.getUniqueId());
    }

    private void loadEffectsConfig(FileConfiguration fileConfiguration) {
        for (String key : fileConfiguration.getConfigurationSection("shop").getKeys(false)) {

            ElytraEffect effect = effectsRegistry.get(key);

            if (effect != null) {
                effect.setPrice(fileConfiguration.getDouble("shop." + key + ".price"));
                effect.setPermission(fileConfiguration.getString("shop." + key + ".permission"));

                String effectName = fileConfiguration.getString("shop." + key + ".name");
                if (effectName != null)
                    effect.setName(ColorHelper.parse(effectName));

                // Handle lore as a list
                List<String> loreLines = fileConfiguration.getStringList("shop." + key + ".lore");
                List<String> translatedLore = new ArrayList<>();
                for (String line : loreLines) {
                    translatedLore.add(ColorHelper.parse(line));
                }
                effect.setLore(translatedLore); // Assuming setLore accepts a List<String>
            }

            effectsRegistry.put(key, effect);
        }
    }

    private void registerEffects() {
        // Effects that work on all versions
        effectsRegistry.put(Constants.Effects.FIRE_TRAIL, new ElytraEffect(Constants.Effects.FIRE_TRAIL,
                "Fire Trail", Material.CAMPFIRE, Particle.FLAME,
                List.of("§7Leave a fiery trail!"), 1000, Constants.Permissions.Effects.FIRE
        ));
        effectsRegistry.put(Constants.Effects.ICE_SHARDS, new ElytraEffect(Constants.Effects.ICE_SHARDS,
                "Ice Shards", Material.ICE, Particle.SNOWFLAKE,
                List.of("§7Shards of ice behind you!"), 1500, Constants.Permissions.Effects.ICE
        ));
        effectsRegistry.put(Constants.Effects.INKY_VOID, new ElytraEffect(Constants.Effects.INKY_VOID,
                "Inky Void", Material.INK_SAC, Particle.SQUID_INK,
                List.of("§7Soar with a trail of darkness."), 1750, Constants.Permissions.Effects.VOID
        ));
        effectsRegistry.put(Constants.Effects.HEART_TRAIL, new ElytraEffect(Constants.Effects.HEART_TRAIL,
                "Heart Trail", Material.POPPY, Particle.HEART,
                List.of("§7Spread love wherever you fly!"), 1250, Constants.Permissions.Effects.HEART
        ));

        effectsRegistry.put(Constants.Effects.SOUL_FIRE, new ElytraEffect(Constants.Effects.SOUL_FIRE,
                "Soul Fire", Material.SOUL_LANTERN, Particle.SOUL_FIRE_FLAME,
                List.of("§7A trail of eerie, blue soul fire."), 1800, Constants.Permissions.Effects.SOULFIRE
        ));

        effectsRegistry.put(Constants.Effects.MUSICAL_FLIGHT, new ElytraEffect(Constants.Effects.MUSICAL_FLIGHT,
                "Musical Flight", Material.JUKEBOX, Particle.NOTE,
                List.of("§7Leave a trail of musical notes as you fly."), 1300, Constants.Permissions.Effects.NOTE
        ));

        effectsRegistry.put(Constants.Effects.CLOUD_SURFER, new ElytraEffect(Constants.Effects.CLOUD_SURFER,
                "Cloud Surfer", Material.WHITE_WOOL, Particle.CLOUD,
                List.of("§7Fly on your own personal cloud."), 800, Constants.Permissions.Effects.CLOUD
        ));

        effectsRegistry.put(Constants.Effects.CRITICAL_AURA, new ElytraEffect(Constants.Effects.CRITICAL_AURA,
                "Critical Aura", Material.DIAMOND_SWORD, Particle.CRIT,
                List.of("§7Fly with the power of a critical hit!"), 2100, Constants.Permissions.Effects.CRIT
        ));

        effectsRegistry.put(Constants.Effects.ENDERS_WAKE, new ElytraEffect(Constants.Effects.ENDERS_WAKE,
                "Ender's Wake", Material.ENDER_EYE, Particle.PORTAL,
                List.of("§7A swirling vortex of the void follows you."), 2800, Constants.Permissions.Effects.PORTAL
        ));

        effectsRegistry.put(Constants.Effects.SPORE_BLOOM, new ElytraEffect(Constants.Effects.SPORE_BLOOM,
                "Spore Bloom", Material.SPORE_BLOSSOM, Particle.SPORE_BLOSSOM_AIR,
                List.of("§7A trail of beautiful, floating spores."), 2800, Constants.Permissions.Effects.SPORE
        ));

        effectsRegistry.put(Constants.Effects.BUBBLE_STREAM, new ElytraEffect(Constants.Effects.BUBBLE_STREAM,
                "Bubble Stream", Material.GLASS_BOTTLE, Particle.BUBBLE_POP,
                List.of("§7A stream of popping bubbles."), 950, Constants.Permissions.Effects.BUBBLE
        ));

        effectsRegistry.put(Constants.Effects.DRAGON_BREATH, new ElytraEffect(Constants.Effects.DRAGON_BREATH,
                "Dragon Breath", Material.DRAGON_EGG, Particle.DRAGON_BREATH,
                List.of("§7The spark of dragon breath."), 2400, Constants.Permissions.Effects.DRAGON
        ));

        effectsRegistry.put(Constants.Effects.DOLPHINS_GRACE, new ElytraEffect(Constants.Effects.DOLPHINS_GRACE,
                "Dolphin's Grace", Material.HEART_OF_THE_SEA, Particle.DOLPHIN,
                List.of("§7Swim through the air with a dolphin's help."), 3200, Constants.Permissions.Effects.DOLPHIN
        ));

        effectsRegistry.put(Constants.Effects.DAMAGE_FLASH, new ElytraEffect(Constants.Effects.DAMAGE_FLASH,
                "Damage Flash", Material.REDSTONE, Particle.DAMAGE_INDICATOR,
                List.of("§7A flash of red damage particles."), 1650, Constants.Permissions.Effects.DAMAGE
        ));

        effectsRegistry.put(Constants.Effects.WAXED_WINGS, new ElytraEffect(Constants.Effects.WAXED_WINGS,
                "Waxed Wings", Material.HONEYCOMB, Particle.WAX_ON,
                List.of("§7A trail of waxy particles."), 1400, Constants.Permissions.Effects.WAX
        ));

        // Effects that depend on server version
        registerVersionDependentEffect(Constants.Effects.SLIME_TRAIL, "§2Slime Trail", Material.SLIME_BALL,
                Map.of(
                        ServerVersion.V_1_18, "SLIME",
                        ServerVersion.V_1_19, "SLIME",
                        ServerVersion.V_1_20, "SLIME",
                        ServerVersion.V_1_21, "ITEM_SLIME"
                ),
                List.of("§7Leave a gooey, green trail behind."), 900, Constants.Permissions.Effects.SLIME);

        registerVersionDependentEffect(Constants.Effects.WITCHES_BREW, "§5Witches Brew", Material.BREWING_STAND,
                Map.of(
                        ServerVersion.V_1_18, "SPELL_WITCH",
                        ServerVersion.V_1_19, "SPELL_WITCH",
                        ServerVersion.V_1_20, "SPELL_WITCH",
                        ServerVersion.V_1_21, "WITCH"
                ),
                List.of("§7A swirling, magical concoction follows you."), 2200, Constants.Permissions.Effects.WITCH);

        registerVersionDependentEffect(Constants.Effects.EXPLOSIVE_TRAIL, "Explosive Trail", Material.TNT,
                Map.of(
                        ServerVersion.V_1_18, "EXPLOSION_NORMAL",
                        ServerVersion.V_1_19, "EXPLOSION_NORMAL",
                        ServerVersion.V_1_20, "EXPLOSION_NORMAL",
                        ServerVersion.V_1_21, "EXPLOSION"
                ),
                List.of("§7A trail of small explosions."), 1100, Constants.Permissions.Effects.EXPLOSION);

        registerVersionDependentEffect(Constants.Effects.SMOKE_SCREEN, "Smoke Screen", Material.BLACK_DYE,
                Map.of(
                        ServerVersion.V_1_18, "SMOKE_LARGE",
                        ServerVersion.V_1_19, "SMOKE_LARGE",
                        ServerVersion.V_1_20, "SMOKE_LARGE",
                        ServerVersion.V_1_21, "LARGE_SMOKE"
                ),
                List.of("§7Create a screen of thick smoke."), 1100, Constants.Permissions.Effects.SMOKE);

        registerVersionDependentEffect(Constants.Effects.TOTEM_BLESSING, "Totem's Blessing", Material.TOTEM_OF_UNDYING,
                Map.of(
                        ServerVersion.V_1_18, "TOTEM",
                        ServerVersion.V_1_19, "TOTEM",
                        ServerVersion.V_1_20, "TOTEM",
                        ServerVersion.V_1_21, "TOTEM_OF_UNDYING"
                ),
                List.of("§7Fly with the blessing of immortality!"), 3500, Constants.Permissions.Effects.TOTEM);


        registerVersionDependentEffect(Constants.Effects.LAVA_DRIP, "Lava Trail", Material.LAVA_BUCKET,
                Map.of(
                        ServerVersion.V_1_18, "DRIP_LAVA",
                        ServerVersion.V_1_19, "DRIP_LAVA",
                        ServerVersion.V_1_20, "DRIP_LAVA",
                        ServerVersion.V_1_21, "DRIPPING_LAVA"
                ),
                List.of("§7Drips of hot magma follow you."), 1900, Constants.Permissions.Effects.LAVADRIP);

        registerVersionDependentEffect(Constants.Effects.WATER_TRAIL, "Water Trail", Material.WATER_BUCKET,
                Map.of(
                        ServerVersion.V_1_18, "DRIP_WATER",
                        ServerVersion.V_1_19, "DRIP_WATER",
                        ServerVersion.V_1_20, "DRIP_WATER",
                        ServerVersion.V_1_21, "DRIPPING_WATER"
                ),
                List.of("§7Trails of water follow you!"), 2000, Constants.Permissions.Effects.WATER);

        registerVersionDependentEffect(Constants.Effects.ARCANE_TRAIL, "Arcane Trail", Material.ENCHANTING_TABLE,
                Map.of(
                        ServerVersion.V_1_18, "ENCHANTMENT_TABLE",
                        ServerVersion.V_1_19, "ENCHANTMENT_TABLE",
                        ServerVersion.V_1_20, "ENCHANTMENT_TABLE",
                        ServerVersion.V_1_21, "ENCHANTMENT_TABLE"
                ),
                List.of("§7Leave a trail of mystical runes."), 2500, Constants.Permissions.Effects.ARCANE);

        registerVersionDependentEffect(Constants.Effects.EMERALD_SPARK, "Emerald Spark", Material.EMERALD,
                Map.of(
                        ServerVersion.V_1_18, "VILLAGER_HAPPY",
                        ServerVersion.V_1_19, "VILLAGER_HAPPY",
                        ServerVersion.V_1_20, "VILLAGER_HAPPY",
                        ServerVersion.V_1_21, "HAPPY_VILLAGER" // Renamed in 1.21
                ),
                List.of("§7Show off with a glittering green trail."), 3000, Constants.Permissions.Effects.EMERALD);


        // Effect only available in 1.20 and later
        if (ServerVersion.getCurrent().ordinal() > ServerVersion.V_1_19.ordinal()) {
            effectsRegistry.put(Constants.Effects.CHERRY_BLOSSOM, new ElytraEffect(Constants.Effects.CHERRY_BLOSSOM,
                    "Waxed Cherry Blossom", Material.CHERRY_LEAVES, Particle.CHERRY_LEAVES,
                    List.of("§7A beautiful trail of cherry petals."), 1550, Constants.Permissions.Effects.CHERRY
            ));
        }
    }

    private void registerVersionDependentEffect(String key, String name, Material material, Map<ServerVersion, String> particleNames,
                                                List<String> lore, int price, String permission) {

        String particleNameToUse = particleNames.getOrDefault(serverVersion, particleNames.get(ServerVersion.V_1_21));

        if (particleNameToUse == null) {
            logger.warning("Could not register effect '" + name + "'. No valid particle name was defined for this version.");
            return;
        }

        try {
            Particle particle = Particle.valueOf(particleNameToUse.toUpperCase());
            effectsRegistry.put(key, new ElytraEffect(key, name, material, particle, lore, price, permission));
        } catch (IllegalArgumentException e) {
            logger.warning("Could not register effect '" + name + "'. Particle '" + particleNameToUse + "' is not valid on this server version.");
        }
    }

    public Map<String, ElytraEffect> getEffectsRegistry(){
        return effectsRegistry;
    }

    private void handleSqlException(SQLException e, Player player, String message) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
        messagesHelper.sendPlayerMessage(player, message);
        logger.log(Level.SEVERE, "A database error occurred for player " + player.getName(), e);
    }
}