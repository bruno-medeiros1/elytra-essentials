package org.bruno.elytraEssentials.utils;

public final class Constants {

    //  Private constructor to prevent instantiation
    private Constants () {}

    public static final class GUI {
        //  Shop GUI Constants
        public static final int SHOP_INVENTORY_SIZE = 45;
        public static final String SHOP_INVENTORY_NAME = "§8Shop";
        public static final int SHOP_PLAYER_HEAD_SLOT = 36;
        public static final int SHOP_PREVIOUS_PAGE_SLOT = 39;
        public static final int SHOP_PAGE_INFO_SLOT = 40;
        public static final int SHOP_NEXT_PAGE_SLOT = 41;
        public static final int SHOP_CLOSE_SLOT = 44;
        public static final int SHOP_ITEMS_PER_PAGE = 14;

        //  Owned Effects GUI Constants
        public static final int EFFECTS_INVENTORY_SIZE = 45;
        public static final String EFFECTS_INVENTORY_NAME = "§8Owned Effects";
        public static final int EFFECTS_ITEM_DISPLAY_LIMIT = 36;
        public static final int EFFECTS_SHOP_SLOT = 36;
        public static final int EFFECTS_PREVIOUS_PAGE_SLOT = 39;
        public static final int EFFECTS_PAGE_INFO_SLOT = 40;
        public static final int EFFECTS_NEXT_PAGE_SLOT = 41;
        public static final int EFFECTS_CLOSE_SLOT = 44;

        // Elytra Forge GUI Constants ---
        public static final int FORGE_INVENTORY_SIZE = 36;
        public static final String FORGE_INVENTORY_NAME = "§8Elytra Forge";
        public static final int FORGE_ELYTRA_SLOT = 10;
        public static final int FORGE_ARMOR_SLOT = 16;
        public static final int FORGE_RESULT_SLOT = 13;
        public static final int FORGE_INFO_ANVIL_SLOT = 31;
        public static final int FORGE_CANCEL_SLOT = 30;
        public static final int FORGE_CONFIRM_SLOT = 32;

    }

    public static final class Permissions {
//        public static final String RELOAD = "elytraessentials.reload";
//        public static final String BOOST = "elytraessentials.elytra.boost";
//        public static final String SUPER_BOOST = "elytraessentials.elytra.superboost";
//        public static final String FORGE_USE = "elytraessentials.forge.use";
    }


    public static final class NBT {
        // You would get the NamespacedKey from a central place
        // public static final NamespacedKey EFFECT_KEY = new NamespacedKey(plugin, "effect_key");
        public static final String ARMORED_ELYTRA_TAG = "armored-elytra";
        public static final String ARMOR_DURABILITY_TAG = "armor-durability";
        public static final String MAX_ARMOR_DURABILITY_TAG = "max-armor-durability";
        public static final String ARMOR_MATERIAL_TAG = "armor-material";
        public static final String PREVIEW_ITEM_TAG = "preview_item";
    }
}