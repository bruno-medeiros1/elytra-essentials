package org.bruno.elytraEssentials.utils;

import java.util.List;

public final class Constants {

    private Constants () {}

    public static final class Inventory {
        public static final int CHESTPLATE_SLOT = 38;
        public static final int MAIN_INV_SIZE = 36;
    }

    public static final class Integrations {
        public static final int BSTATS_ID = 26164;
        public static final int SPIGOT_RESOURCE_ID = 126002;
    }

    public static final class GUI {
        // Shop GUI Constants
        public static final int SHOP_INVENTORY_SIZE = 45;
        public static final String SHOP_INVENTORY_NAME = "§8Shop";
        public static final int SHOP_PLAYER_HEAD_SLOT = 36;
        public static final int SHOP_PREVIOUS_PAGE_SLOT = 39;
        public static final int SHOP_PAGE_INFO_SLOT = 40;
        public static final int SHOP_NEXT_PAGE_SLOT = 41;
        public static final int SHOP_CLOSE_SLOT = 44;
        public static final int SHOP_ITEMS_PER_PAGE = 14;

        public static final List<Integer> SHOP_ITEM_SLOTS = List.of(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25
        );

        // Owned Effects GUI Constants
        public static final int EFFECTS_INVENTORY_SIZE = 45;
        public static final String EFFECTS_INVENTORY_NAME = "§8Owned Effects";
        public static final int EFFECTS_ITEM_DISPLAY_LIMIT = 36;
        public static final int EFFECTS_SHOP_SLOT = 36;
        public static final int EFFECTS_PREVIOUS_PAGE_SLOT = 39;
        public static final int EFFECTS_PAGE_INFO_SLOT = 40;
        public static final int EFFECTS_NEXT_PAGE_SLOT = 41;
        public static final int EFFECTS_CLOSE_SLOT = 44;

        // Elytra Forge GUI Constants
        public static final int FORGE_INVENTORY_SIZE = 36;
        public static final String FORGE_INVENTORY_NAME = "§8Elytra Forge";
        public static final int FORGE_ELYTRA_SLOT = 10;
        public static final int FORGE_ARMOR_SLOT = 16;
        public static final int FORGE_RESULT_SLOT = 13;
        public static final int FORGE_INFO_ANVIL_SLOT = 31;
        public static final int FORGE_CANCEL_SLOT = 30;
        public static final int FORGE_CONFIRM_SLOT = 32;

        // Achievements GUI Constants
        public static final int ACHIEVEMENTS_INVENTORY_SIZE = 54;
        public static final String ACHIEVEMENTS_INVENTORY_NAME = "§8Achievements";
        public static final int ACHIEVEMENTS_ITEMS_PER_PAGE = 21;

        // The specific slots where achievement items will be placed
        public static final List<Integer> ACHIEVEMENT_ITEM_SLOTS = List.of(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        );

        // New Control Row Layout
        public static final int ACHIEVEMENTS_FILTER_SLOT = 45;
        public static final int ACHIEVEMENTS_PREVIOUS_PAGE_SLOT = 48;
        public static final int ACHIEVEMENTS_PAGE_INFO_SLOT = 49;
        public static final int ACHIEVEMENTS_NEXT_PAGE_SLOT = 50;
        public static final int ACHIEVEMENTS_CLOSE_SLOT = 53;

        // Upgrade GUI Constants
        public static final int UPGRADE_INVENTORY_SIZE = 45;
        public static final int UPGRADE_CLOSE_SLOT = 44;
    }

    public static final class Permissions {
        // Wildcard Permissions
        public static final String ALL = "elytraessentials.*";
        public static final String ALL_COMMANDS = "elytraessentials.command.*";
        public static final String ALL_BYPASS = "elytraessentials.bypass.*";
        public static final String ALL_EFFECTS = "elytraessentials.effect.*";

        // Command Permissions
        public static final String CMD_HELP = "elytraessentials.command.help";
        public static final String CMD_RELOAD = "elytraessentials.command.reload";
        public static final String CMD_FLIGHT_TIME = "elytraessentials.command.flighttime";
        public static final String CMD_SHOP = "elytraessentials.command.shop";
        public static final String CMD_EFFECTS = "elytraessentials.command.effects";
        public static final String CMD_EFFECTS_CLEAR = "elytraessentials.effects.clear";
        public static final String CMD_EFFECTS_GIVE = "elytraessentials.effects.give";
        public static final String CMD_EFFECTS_REMOVE = "elytraessentials.effects.remove";
        public static final String CMD_EFFECTS_LIST = "elytraessentials.effects.list";
        public static final String CMD_STATS = "elytraessentials.command.stats";
        public static final String CMD_STATS_OTHERS = "elytraessentials.command.stats.others";
        public static final String CMD_STATS_RESET = "elytraessentials.command.stats.reset";
        public static final String CMD_TOP = "elytraessentials.command.top";
        public static final String CMD_FORGE = "elytraessentials.command.forge";
        public static final String CMD_ARMOR = "elytraessentials.command.armor";
        public static final String CMD_REPAIR = "elytraessentials.command.armor.repair";
        public static final String CMD_IMPORT_DB = "elytraessentials.command.importdb";
        public static final String CMD_ACHIEVEMENTS = "elytraessentials.command.achievements";
        public static final String CMD_TANDEM_INVITE = "elytraessentials.command.tandem.invite";
        public static final String CMD_TANDEM_ACCEPT = "elytraessentials.command.tandem.accept";
        public static final String CMD_TANDEM_LEAVE = "elytraessentials.command.tandem.leave";
        public static final String CMD_UPGRADE = "elytraessentials.command.upgrade";

        // Bypass Permissions
        public static final String BYPASS_SPEED_LIMIT = "elytraessentials.bypass.speedlimit";
        public static final String BYPASS_TIME_LIMIT = "elytraessentials.bypass.timelimit";
        public static final String BYPASS_EQUIPMENT = "elytraessentials.bypass.equipment";
        public static final String BYPASS_BOOST_COOLDOWN = "elytraessentials.bypass.boostcooldown";
        public static final String BYPASS_COMBAT_TAG = "elytraessentials.bypass.combattag";

        /**
         * Constants for individual cosmetic effect permissions.
         */
        public static final class Effects {
            public static final String FIRE = "elytraessentials.effect.fire";
            public static final String WATER = "elytraessentials.effect.water";
            public static final String ICE = "elytraessentials.effect.ice";
            public static final String VOID = "elytraessentials.effect.void";
            public static final String HEART = "elytraessentials.effect.heart";
            public static final String ARCANE = "elytraessentials.effect.arcane";
            public static final String EMERALD = "elytraessentials.effect.emerald";
            public static final String WITCH = "elytraessentials.effect.witch";
            public static final String SOULFIRE = "elytraessentials.effect.soulfire";
            public static final String NOTE = "elytraessentials.effect.note";
            public static final String CLOUD = "elytraessentials.effect.cloud";
            public static final String SLIME = "elytraessentials.effect.slime";
            public static final String CRIT = "elytraessentials.effect.crit";
            public static final String PORTAL = "elytraessentials.effect.portal";
            public static final String SPORE = "elytraessentials.effect.spore";
            public static final String BUBBLE = "elytraessentials.effect.bubble";
            public static final String DRAGON = "elytraessentials.effect.dragon";
            public static final String WAX = "elytraessentials.effect.wax";
            public static final String EXPLOSION = "elytraessentials.effect.explosion";
            public static final String SMOKE = "elytraessentials.effect.smoke";
            public static final String TOTEM = "elytraessentials.effect.totem";
            public static final String LAVADRIP = "elytraessentials.effect.lavadrip";
            public static final String CHERRY = "elytraessentials.effect.cherry";
            public static final String DOLPHIN = "elytraessentials.effect.dolphin";
            public static final String DAMAGE = "elytraessentials.effect.damage";
        }

        // Feature Permissions
        public static final String NOTIFY_UPDATE = "elytraessentials.update.notify";
        public static final String BOOST = "elytraessentials.elytra.boost";
        public static final String SUPER_BOOST = "elytraessentials.elytra.superboost";
        public static final String CHARGED_JUMP = "elytraessentials.elytra.chargedjump";
        public static final String AUTO_DEPLOY = "elytraessentials.elytra.autodeploy";
        public static final String TANDEM_FLIGHT = "elytraessentials.elytra.tandemflight";
    }

    public static final class Effects {
        public static final String FIRE_TRAIL = "FIRE_TRAIL";
        public static final String ICE_SHARDS = "ICE_SHARDS";
        public static final String INKY_VOID = "INKY_VOID";
        public static final String HEART_TRAIL = "HEART_TRAIL";
        public static final String SOUL_FIRE = "SOUL_FIRE";
        public static final String MUSICAL_FLIGHT = "MUSICAL_FLIGHT";
        public static final String CLOUD_SURFER = "CLOUD_SURFER";
        public static final String CRITICAL_AURA = "CRITICAL_AURA";
        public static final String ENDERS_WAKE = "ENDERS_WAKE";
        public static final String SPORE_BLOOM = "SPORE_BLOOM";
        public static final String BUBBLE_STREAM = "BUBBLE_STREAM";
        public static final String DRAGON_BREATH = "DRAGON_BREATH";
        public static final String DOLPHINS_GRACE = "DOLPHINS_GRACE";
        public static final String DAMAGE_FLASH = "DAMAGE_FLASH";
        public static final String WAXED_WINGS = "WAXED_WINGS";
        public static final String SLIME_TRAIL = "SLIME_TRAIL";
        public static final String WITCHES_BREW = "WITCHES_BREW";
        public static final String EXPLOSIVE_TRAIL = "EXPLOSIVE_TRAIL";
        public static final String SMOKE_SCREEN = "SMOKE_SCREEN";
        public static final String TOTEM_BLESSING = "TOTEM_BLESSING";
        public static final String LAVA_DRIP = "LAVA_DRIP";
        public static final String WATER_TRAIL = "WATER_TRAIL";
        public static final String ARCANE_TRAIL = "ARCANE_TRAIL";
        public static final String EMERALD_SPARK = "EMERALD_SPARK";
        public static final String CHERRY_BLOSSOM = "CHERRY_BLOSSOM";
    }

    public static final class Files {
        public static final String MESSAGES = "messages.yml";
        public static final String SHOP = "shop.yml";
        public static final String ACHIEVEMENTS = "achievements.yml";

        public static final String SQLITE_DB_NAME = "elytraessentials.db";
        public static final String DB_FOLDER = "database";
        public static final String DB_BACKUP_FOLDER = "backups";
    }

    public static final class Database {

        public static final class Tables {
            public static final String ELYTRA_FLIGHT_TIME = "elytra_flight_time";
            public static final String OWNED_EFFECTS = "owned_effects";
            public static final String PLAYER_STATS = "player_stats";
            public static final String PLAYER_ACHIEVEMENTS = "player_achievements";
        }
    }

    public static final class NBT {
        public static final String ARMORED_ELYTRA_TAG = "armored-elytra";
        public static final String ARMOR_DURABILITY_TAG = "armor-durability";
        public static final String MAX_ARMOR_DURABILITY_TAG = "max-armor-durability";
        public static final String ARMOR_MATERIAL_TAG = "armor-material";
        public static final String PREVIEW_ITEM_TAG = "preview_item";
        public static final String DAMAGE_ABSORBED_TAG = "damage-absorbed";
        public static final String PLATING_SHATTERED_TAG = "plating-shattered";
        public static final String FORGED_BY_TAG = "forged-by";

        public static final String EFFECT_KEY = "effect_key";
        public static final String EFFECT_PERMISSION_KEY = "effect_permission_key";

        public static final String MAX_VELOCITY_UPGRADE_TAG = "max_velocity_upgrade";
        public static final String FLIGHT_EFFICIENCY_UPGRADE_TAG = "flight_efficiency_upgrade";
        public static final String BOOST_POWER_UPGRADE_TAG = "boost_power_upgrade";
        public static final String ARMOR_DURABILITY_UPGRADE_TAG = "armor_durability_upgrade";
        public static final String KINETIC_RESISTANCE_UPGRADE_TAG = "kinetic_resistance_upgrade";
        public static final String ARMOR_PROTECTION_UPGRADE_TAG = "armor_protection_upgrade";
        public static final String ARMOR_TOUGHNESS_UPGRADE_TAG = "armor_toughness_upgrade";
    }

    public static final class Skull {
        public static final String ARROW_RIGHT_ACTIVE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19";
        public static final String ARROW_LEFT_ACTIVE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==";
        public static final String ARROW_RIGHT_INACTIVE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWRiNTMyYjVjY2VkNDZiNGI1MzVlY2UxNmVjZWQ3YmJjNWNhYzU1NTk0ZDYxZThiOGY4ZWFjNDI5OWM5ZmMifX19";
        public static final String ARROW_LEFT_INACTIVE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWRiNTMyYjVjY2VkNDZiNGI1MzVlY2UxNmVjZWQ3YmJjNWNhYzU1NTk0ZDYxZThiOGY4ZWFjNDI5OWM5ZmMifX19";
        public static final String GREEN_CHECKMARK = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDMxMmNhNDYzMmRlZjVmZmFmMmViMGQ5ZDdjYzdiNTVhNTBjNGUzOTIwZDkwMzcyYWFiMTQwNzgxZjVkZmJjNCJ9fX0=";
        public static final String RED_X = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmViNTg4YjIxYTZmOThhZDFmZjRlMDg1YzU1MmRjYjA1MGVmYzljYWI0MjdmNDYwNDhmMThmYzgwMzQ3NWY3In19fQ==";
    }
}