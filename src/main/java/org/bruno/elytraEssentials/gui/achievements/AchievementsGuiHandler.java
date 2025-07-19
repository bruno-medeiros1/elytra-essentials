package org.bruno.elytraEssentials.gui.achievements;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.AchievementsHandler;
import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.StatType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AchievementsGuiHandler {
    private final Logger logger;
    private final DatabaseHandler databaseHandler;
    private final FoliaHelper foliaHelper;
    private final MessagesHelper messagesHelper;
    private final AchievementsHandler achievementsHandler;
    private final StatsHandler statsHandler;

    private static class PlayerGuiState {
        int page;
        StatType filter;

        PlayerGuiState(int page, StatType filter) {
            this.page = page;
            this.filter = filter;
        }
    }

    private final Map<UUID, PlayerGuiState> playerStates = new HashMap<>();

    private final List<StatType> filterCycle = Arrays.asList(
        StatType.UNKNOWN, // Represents "All"
        StatType.TOTAL_DISTANCE,
        StatType.TOTAL_FLIGHT_TIME,
        StatType.LONGEST_FLIGHT,
        StatType.BOOSTS_USED,
        StatType.SAVES
    );


    public AchievementsGuiHandler(Logger logger, DatabaseHandler databaseHandler, FoliaHelper foliaHelper, MessagesHelper messagesHelper,
                                  AchievementsHandler achievementsHandler, StatsHandler statsHandler) {
        this.logger = logger;
        this.databaseHandler = databaseHandler;
        this.foliaHelper = foliaHelper;
        this.messagesHelper = messagesHelper;
        this.achievementsHandler = achievementsHandler;
        this.statsHandler = statsHandler;
    }

    /**
     * Asynchronously builds and opens the achievements GUI for a player.
     */
    public void open(Player player, int page, StatType filter) {
        playerStates.put(player.getUniqueId(), new PlayerGuiState(page, filter));

        // Fetch player's unlocked achievements from the DB on a background thread
        foliaHelper.runAsyncTask(() -> {
            try {
                Set<String> unlockedAchievements = databaseHandler.getUnlockedAchievementIds(player.getUniqueId());

                // Now that we have the data, build and open the GUI on the main thread
                foliaHelper.runTaskOnMainThread(() -> {
                    Inventory gui = Bukkit.createInventory(new AchievementsHolder(), Constants.GUI.ACHIEVEMENTS_INVENTORY_SIZE, Constants.GUI.ACHIEVEMENTS_INVENTORY_NAME);
                    populateItems(gui, player, page, filter);
                    addControlButtons(gui, page, filter);
                    player.openInventory(gui);
                });
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to fetch achievements for player " + player.getName(), e);
                foliaHelper.runTaskOnMainThread(() ->
                        messagesHelper.sendPlayerMessage(player,"&cCould not load your achievement data."));
            }
        });
    }

    /**
     * Handles all click events for the achievements GUI.
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        PlayerGuiState currentState = playerStates.getOrDefault(player.getUniqueId(), new PlayerGuiState(0, StatType.UNKNOWN));
        int clickedSlot = event.getSlot();

        switch (clickedSlot) {
            case Constants.GUI.ACHIEVEMENTS_PREVIOUS_PAGE_SLOT:
                if (currentState.page > 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);

                    playerStates.put(player.getUniqueId(), new PlayerGuiState(currentState.page - 1, currentState.filter));

                    this.open(player, currentState.page - 1, currentState.filter);
                }
                break;
            case Constants.GUI.ACHIEVEMENTS_NEXT_PAGE_SLOT:
                long totalItems = achievementsHandler.getAllAchievements().stream()
                        .filter(ach -> currentState.filter == StatType.UNKNOWN || ach.type() == currentState.filter)
                        .count();
                int totalPages = (int) Math.ceil((double) totalItems / Constants.GUI.ACHIEVEMENTS_ITEMS_PER_PAGE);
                if (totalPages == 0) totalPages = 1;

                if (currentState.page < totalPages - 1) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);

                    playerStates.put(player.getUniqueId(), new PlayerGuiState(currentState.page + 1, currentState.filter));

                    this.open(player, currentState.page + 1, currentState.filter);
                }
                break;
            case Constants.GUI.ACHIEVEMENTS_CLOSE_SLOT:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
                player.closeInventory();
                break;
            case Constants.GUI.ACHIEVEMENTS_FILTER_SLOT:
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.0f);

                int currentIndex = filterCycle.indexOf(currentState.filter);
                int nextIndex = (currentIndex + 1) % filterCycle.size();
                StatType newFilter = filterCycle.get(nextIndex);

                playerStates.put(player.getUniqueId(), new PlayerGuiState(0, newFilter));

                // Open page 0 of the new filter
                this.open(player, 0, newFilter);
                break;
        }
    }

    private void populateItems(Inventory gui, Player player, int page, StatType filter) {
        //  Filtering Logic
        List<AchievementsHandler.Achievement> achievementsToDisplay = achievementsHandler.getAllAchievements().stream()
                .filter(ach -> filter == StatType.UNKNOWN || ach.type() == filter)
                .sorted(Comparator.comparingDouble(AchievementsHandler.Achievement::value))
                .toList();

        Set<String> unlockedAchievements;
        try {
            unlockedAchievements = databaseHandler.getUnlockedAchievementIds(player.getUniqueId());
        } catch (SQLException e) {
            messagesHelper.sendPlayerMessage(player,"&cCould not load your achievement data.");
            logger.log(Level.SEVERE, "Failed to fetch achievements for player " + player.getName(), e);
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
                double playerValue = statsHandler.getStatValue(player, achievement.type());
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
        long totalItems = achievementsHandler.getAllAchievements().stream()
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
