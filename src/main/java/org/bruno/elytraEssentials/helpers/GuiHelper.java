package org.bruno.elytraEssentials.helpers;

import org.bruno.elytraEssentials.utils.StatType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GuiHelper {

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
            return createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§aPrevious Page");
        } else {
            return createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§cPrevious Page", "§7You are on the first page.");
        }
    }

    public static ItemStack createNextPageButton(boolean isActive) {
        if (isActive) {
            return createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§aNext Page");
        } else {
            return createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§cNext Page", "§7You are on the last page.");
        }
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
