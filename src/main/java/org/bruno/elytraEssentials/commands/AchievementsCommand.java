package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.gui.AchievementsHolder;
import org.bruno.elytraEssentials.handlers.AchievementsHandler;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class AchievementsCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public AchievementsCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        if (!PermissionsHelper.hasAchievementsPermission(player)){
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return true;
        }

        openAchievementsGUI(player, 0, StatType.UNKNOWN);
        return true;
    }

    public void openAchievementsGUI(Player player, int page, StatType filter) {
        plugin.getAchievementsGuiListener().setPlayerState(player.getUniqueId(), page, filter);

        Inventory gui = Bukkit.createInventory(new AchievementsHolder(), Constants.GUI.ACHIEVEMENTS_INVENTORY_SIZE, Constants.GUI.ACHIEVEMENTS_INVENTORY_NAME);

        populateItems(gui, player, page, filter);
        addControlButtons(gui, page, filter);

        player.openInventory(gui);
    }

    private void populateItems(Inventory gui, Player player, int page, StatType filter) {
        AchievementsHandler achievementsHandler = plugin.getAchievementsHandler();

        //  Filtering Logic
        List<AchievementsHandler.Achievement> achievementsToDisplay = achievementsHandler.getAllAchievements().stream()
                .filter(ach -> filter == StatType.UNKNOWN || ach.type() == filter)
                .sorted(Comparator.comparingDouble(AchievementsHandler.Achievement::value))
                .toList();

        Set<String> unlockedAchievements;
        try {
            unlockedAchievements = plugin.getDatabaseHandler().getUnlockedAchievementIds(player.getUniqueId());
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "Could not load your achievement data.");
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch achievements for player " + player.getName(), e);
            return;
        }

        //  Populate items using the defined list of slots
        int startIndex = page * Constants.GUI.ACHIEVEMENTS_ITEMS_PER_PAGE;
        for (int i = 0; i < Constants.GUI.ACHIEVEMENTS_ITEMS_PER_PAGE; i++) {
            int listIndex = startIndex + i;
            // Get the correct slot from our constants list
            int slot = Constants.GUI.ACHIEVEMENT_ITEM_SLOTS.get(i);

            if (listIndex < achievementsToDisplay.size()) {

                // If there is an achievement for this slot, create and place it
                AchievementsHandler.Achievement achievement = achievementsToDisplay.get(listIndex);

                boolean isUnlocked = unlockedAchievements.contains(achievement.id());
                ItemStack displayItem = createAchievementItem(player, achievement, isUnlocked);
                gui.setItem(slot, displayItem);
            }
        }

        //  Fill all other empty slots with a border
        ItemStack fillerPane = GuiHelper.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < Constants.GUI.ACHIEVEMENTS_INVENTORY_SIZE; i++) {
            // Check if the slot is an item slot or a control slot
            boolean isItemSlot = Constants.GUI.ACHIEVEMENT_ITEM_SLOTS.contains(i);
            boolean isControlSlot = (i >= 45); // Assuming bottom row is for controls

            // If the slot is empty and not an item slot or control slot, fill it
            if (gui.getItem(i) == null && !isItemSlot && !isControlSlot) {
                gui.setItem(i, fillerPane);
            }
        }
    }

    private ItemStack createAchievementItem(Player player, AchievementsHandler.Achievement achievement, boolean isUnlocked) {
        Material material = isUnlocked ? Material.YELLOW_DYE : achievement.displayItem();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorHelper.parse(achievement.name()));

            List<String> lore = new ArrayList<>();
            lore.add(ColorHelper.parse(achievement.description()));
            lore.add("");

            if (isUnlocked) {
                lore.add("§a✔ Completed!");
            } else {
                double playerValue = plugin.getStatsHandler().getStatValue(player, achievement.type());
                lore.add("§eYour Progress:");
                lore.add(createProgressBar(playerValue, achievement.value()));
            }

            if (!achievement.rewards().isEmpty()) {
                lore.add("");
                lore.add("§6Rewards:");
                for (String rewardLine : achievement.rewards()) {
                    lore.add(ColorHelper.parse(rewardLine));
                }
            }

            if (isUnlocked) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private String createProgressBar(double current, double goal) {
        double percentage = (goal > 0) ? current / goal : 1.0;
        if (percentage > 1.0) percentage = 1.0;

        int totalBars = 20;
        int greenBars = (int) (totalBars * percentage);

        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < totalBars; i++) {
            if (i == greenBars) {
                bar.append("§f▆");
            }
            bar.append("▆");
        }
        bar.append(String.format(" §e(%.0f/%.0f)", current, goal));
        return bar.toString();
    }

    private void addControlButtons(Inventory gui, int page, StatType filter) {
        // Calculate total pages based on the FILTERED list
        long totalItems = plugin.getAchievementsHandler().getAllAchievements().stream()
                .filter(ach -> filter == StatType.UNKNOWN || ach.type() == filter)
                .count();
        int totalPages = (int) Math.ceil((double) totalItems / Constants.GUI.ACHIEVEMENTS_ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        // Define the order of filters for cycling
        List<StatType> filterCycle = Arrays.asList(
                StatType.UNKNOWN, StatType.TOTAL_DISTANCE, StatType.TOTAL_FLIGHT_TIME,
                StatType.LONGEST_FLIGHT, StatType.BOOSTS_USED, StatType.SAVES
        );

        // Use the GuiHelper method to create the filter button
        gui.setItem(Constants.GUI.ACHIEVEMENTS_FILTER_SLOT, GuiHelper.createFilterButton(filter, filterCycle));

        // Use the GuiHelper methods for page navigation
        gui.setItem(Constants.GUI.ACHIEVEMENTS_PREVIOUS_PAGE_SLOT, GuiHelper.createPreviousPageButton(page > 0));
        gui.setItem(Constants.GUI.ACHIEVEMENTS_PAGE_INFO_SLOT, GuiHelper.createPageInfoItem(page + 1, totalPages));
        gui.setItem(Constants.GUI.ACHIEVEMENTS_NEXT_PAGE_SLOT, GuiHelper.createNextPageButton(page < totalPages - 1));
        gui.setItem(Constants.GUI.ACHIEVEMENTS_CLOSE_SLOT, GuiHelper.createCloseButton());
    }
}
