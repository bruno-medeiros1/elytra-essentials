package org.bruno.elytraEssentials.helpers;

import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.StatType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GuiHelper {
    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private GuiHelper() {}

    /**
     * Fills the shop with the available elytra effects.
     */
    public static ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * A specialized helper method to create the player head item.
     */
    public static ItemStack createPlayerHead(Player player, String name, String... lore) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(name);
            skullMeta.setLore(Arrays.asList(lore));
            playerHead.setItemMeta(skullMeta);
        }
        return playerHead;
    }

    public static ItemStack createPreviousPageButton(boolean isActive) {
        if (isActive) {
            try {
                return createCustomHead(Constants.Skull.ARROW_LEFT_ACTIVE, "§aPrevious Page", "§7Click to go to the previous page.");
            } catch (Throwable e) {
                return createGuiItem(Material.RED_STAINED_GLASS_PANE, "§aPrevious Page", "§7Click to go to the previous page.");
            }
        } else {
            try {
                return createCustomHead(Constants.Skull.ARROW_LEFT_INACTIVE, "§aPrevious Page", "§7Click to go to the previous page.");
            } catch (Throwable e) {
                return createGuiItem(Material.RED_STAINED_GLASS_PANE, "§aPrevious Page", "§7Click to go to the previous page.");
            }
        }
    }

    public static ItemStack createNextPageButton(boolean isActive) {
        if (isActive) {
            try {
                return createCustomHead(Constants.Skull.ARROW_RIGHT_ACTIVE, "§aNext Page", "§7Click to go to the next page.");
            } catch (Throwable e) {
                return createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§aNext Page", "§7Click to go to the next page.");
            }
        } else {
            try {
                return createCustomHead(Constants.Skull.ARROW_RIGHT_INACTIVE, "§cNext Page", "§7You are on the last page.");
            } catch (Throwable e) {
                return createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§cNext Page", "§7You are on the last page.");
            }
        }
    }

    public static ItemStack createAcceptButton(String action, List<String> lore){
        try {
            return createCustomHead(Constants.Skull.GREEN_CHECKMARK, "§aConfirm " + action, lore.toArray(new String[0]));
        } catch (Throwable e) {
            return createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§aConfirm " + action, lore.toArray(new String[0]));
        }
    }

    public static ItemStack createCancelButton() {
        try {
            return createCustomHead(Constants.Skull.RED_X, "§cCancel", "§7Click here to cancel the operation.");
        } catch (Throwable e) {
            return createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cCancel", "§7Click here to cancel the operation.");
        }
    }

    /**
     * Creates a player head with a custom texture from a Base64 string.
     * This method uses the modern PlayerProfile API and does not require reflection or external libraries.
     * Only compatible with 1.19 and above.
     */
    private static ItemStack createCustomHead(String base64, String name, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            // Create a new, blank player profile.
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            // Decode the Base64 string to get the texture URL.
            byte[] decoded = Base64.getDecoder().decode(base64);
            String decodedString = new String(decoded, StandardCharsets.UTF_8);

            // Use regex to safely extract the URL from the JSON data.
            Matcher matcher = URL_PATTERN.matcher(decodedString);
            if (matcher.find()) {
                String urlString = matcher.group(1);
                textures.setSkin(new URL(urlString));
            } else {
                throw new IllegalArgumentException("Could not find URL in decoded texture data.");
            }

            // Apply the textured profile to the skull meta.
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);

            // Set display name and lore.
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            head.setItemMeta(meta);
        }
        catch (Exception e) {
            // Fallback to a default item if the texture fails to load
            return createGuiItem(Material.BARRIER, name, lore);
        }
        catch (Throwable e){
            throw e; // Re-throw any other unexpected errors
        }

        return head;
    }

    public static ItemStack createPageInfoItem(int currentPage, int totalPages) {
        return createGuiItem(Material.COMPASS, String.format("§ePage %d / %d", currentPage, totalPages));
    }

    public static ItemStack createCloseButton() {
        return createGuiItem(Material.BARRIER, "§cClose", "§7Click to close the menu.");
    }

    public static ItemStack createFilterButton(StatType currentFilter, List<StatType> filterCycle) {
        String filterName = (currentFilter == StatType.UNKNOWN) ? "All" : getCapitalizedName(currentFilter.name());

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to cycle through categories.");
        lore.add("");
        for (StatType type : filterCycle) {
            String name = (type == StatType.UNKNOWN) ? "All" : getCapitalizedName(type.name());
            if (type == currentFilter) {
                lore.add("§a» " + name + " §a«");
            } else {
                lore.add("§7" + name);
            }
        }

        return createGuiItem(Material.HOPPER, "§eFilter: §b" + filterName, lore.toArray(new String[0]));
    }

    /**
     * In API versions 1.20.6 and earlier, InventoryView is a class.
     * In versions 1.21 and later, it is an interface.
     * This method uses reflection to get the top Inventory object from the
     * InventoryView associated with an InventoryEvent, to avoid runtime errors.
     * @param event The generic InventoryEvent with an InventoryView to inspect.
     * @return The top Inventory object from the event's InventoryView.
     */
    public static Inventory getTopInventory(InventoryEvent event) {
        try {
            Object view = event.getView();
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            getTopInventory.setAccessible(true);
            return (Inventory) getTopInventory.invoke(view);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A method that will return the Stat type in a more
     * user-friendly way. (e.g. TOTAL_DISTANCE -> Total Distance)
     */
    private static String getCapitalizedName(String name) {
        String[] words = name.toLowerCase().replace("_", " ").split(" ");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            capitalized.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return capitalized.toString().trim();
    }
}
