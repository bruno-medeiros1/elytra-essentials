package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.commands.AchievementsCommand;
import org.bruno.elytraEssentials.gui.AchievementsHolder;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.StatType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class AchievementsGuiListener implements Listener {

    private final ElytraEssentials plugin;
    private final AchievementsCommand achievementsCommand;

    // This class will hold the state for each player viewing the GUI
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

    public AchievementsGuiListener(ElytraEssentials plugin, AchievementsCommand achievementsCommand) {
        this.plugin = plugin;
        this.achievementsCommand = achievementsCommand;
    }

    /**
     * Stores the current page and filter for a player viewing the GUI.
     * This is called by AchievementsCommand when the GUI is opened.
     */
    public void setPlayerState(UUID uuid, int page, StatType filter) {
        playerStates.put(uuid, new PlayerGuiState(page, filter));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up the map when a player leaves to prevent memory leaks
        playerStates.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof AchievementsHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        PlayerGuiState currentState = playerStates.getOrDefault(player.getUniqueId(), new PlayerGuiState(0, StatType.UNKNOWN));
        int clickedSlot = event.getSlot();

        switch (clickedSlot) {
            case Constants.GUI.ACHIEVEMENTS_PREVIOUS_PAGE_SLOT:
                if (currentState.page > 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);

                    playerStates.put(player.getUniqueId(), new PlayerGuiState(currentState.page - 1, currentState.filter));

                    plugin.openAchievementsGUI(player, currentState.page - 1, currentState.filter);
                }
                break;
            case Constants.GUI.ACHIEVEMENTS_NEXT_PAGE_SLOT:
                long totalItems = plugin.getAchievementsHandler().getAllAchievements().stream()
                        .filter(ach -> currentState.filter == StatType.UNKNOWN || ach.type() == currentState.filter)
                        .count();
                int totalPages = (int) Math.ceil((double) totalItems / Constants.GUI.ACHIEVEMENTS_ITEMS_PER_PAGE);
                if (totalPages == 0) totalPages = 1;

                if (currentState.page < totalPages - 1) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);

                    playerStates.put(player.getUniqueId(), new PlayerGuiState(currentState.page + 1, currentState.filter));

                    plugin.openAchievementsGUI(player, currentState.page + 1, currentState.filter);
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
                plugin.openAchievementsGUI(player, 0, newFilter);
                break;
        }
    }
}
