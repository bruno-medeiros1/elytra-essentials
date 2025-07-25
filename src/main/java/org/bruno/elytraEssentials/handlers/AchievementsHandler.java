package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bruno.elytraEssentials.utils.StatType;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AchievementsHandler {

    private final DatabaseHandler databaseHandler;
    private final StatsHandler statsHandler;
    private final FoliaHelper foliaHelper;
    private final MessagesHelper messagesHelper;
    private final FileConfiguration fileConfiguration;
    private final Logger logger;
    private final MessagesHandler messagesHandler;

    private final Map<String, Achievement> achievements = new HashMap<>();
    private CancellableTask checkTask;

    public record Achievement(String id, String name, StatType type, double value, String description,
                              Material displayItem,
                              String message, List<String> commands, List<String> rewards, boolean broadcast) {}

    public AchievementsHandler(DatabaseHandler databaseHandler, StatsHandler statsHandler, FoliaHelper foliaHelper, MessagesHelper messagesHelper,
                               FileConfiguration fileConfiguration, Logger logger, MessagesHandler messagesHandler) {
        this.databaseHandler = databaseHandler;
        this.statsHandler = statsHandler;
        this.foliaHelper = foliaHelper;
        this.messagesHelper = messagesHelper;
        this.fileConfiguration = fileConfiguration;
        this.logger = logger;
        this.messagesHandler = messagesHandler;

        loadAchievements();
    }

    /**
     * Loads all achievement definitions from the achievements.yml file into memory.
     */
    public void loadAchievements() {
        achievements.clear();
        ConfigurationSection achievementsSection = fileConfiguration.getConfigurationSection("achievements");
        if (achievementsSection == null) {
            logger.warning("No 'achievements' section found in achievements.yml.");
            return;
        }

        for (String key : achievementsSection.getKeys(false)) {
            String path = "achievements." + key;
            try {
                StatType type = StatType.valueOf(fileConfiguration.getString(path + ".type", "UNKNOWN").toUpperCase());
                String name = fileConfiguration.getString(path + ".name", "Unnamed Achievement");
                double value = fileConfiguration.getDouble(path + ".value");
                String description = fileConfiguration.getString(path + ".description", "&7No description provided.");
                Material displayItem = Material.matchMaterial(fileConfiguration.getString(path + ".display-item", "BARRIER"));

                String message = fileConfiguration.getString(path + ".message", "");
                List<String> commands = fileConfiguration.getStringList(path + ".commands");
                List<String> rewards = fileConfiguration.getStringList(path + ".rewards");
                boolean broadcast = fileConfiguration.getBoolean(path + ".broadcast", true);

                achievements.put(key, new Achievement(key, name, type, value, description, displayItem, message, commands, rewards, broadcast));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load achievement '" + key + "'. Please check its format in achievements.yml.", e);
            }
        }
    }

    /**
     * Starts the repeating task that checks for new achievements for online players.
     */
    public void start() {
        if (checkTask != null) return;

        // Use the Folia-safe global timer to run our check every minute.
        this.checkTask = foliaHelper.runTaskTimerGlobal(() -> {
            // The check itself involves database calls, so run it asynchronously.
            foliaHelper.runAsyncTask(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkAndAwardAchievements(player);
                }
            });
        }, 20L * 60, 20L * 60); // Delay 1 min, repeat every 1 min
    }

    /**
     * Stops the achievement checking task. Renamed for consistency.
     */
    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    public Collection<Achievement> getAllAchievements() {
        return achievements.values();
    }

    /**
     * The core logic. Checks a player's stats against all achievements and awards them if necessary.
     */
    private void checkAndAwardAchievements(Player player) {
        PlayerStats stats = statsHandler.getStats(player);
        if (stats == null) return;

        for (Achievement achievement : achievements.values()) {
            try {
                if (databaseHandler.hasAchievement(player.getUniqueId(), achievement.id())) {
                    continue;
                }

                double playerValue = getStatValue(stats, achievement.type());
                if (playerValue >= achievement.value()) {
                    awardAchievement(player, achievement);
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error while checking achievements for " + player.getName(), e);
            }
        }
    }

    private void awardAchievement(Player player, Achievement achievement) throws SQLException {
        // Save the achievement to the database so they don't get it again.
        databaseHandler.addAchievement(player.getUniqueId(), achievement.id());

        // Schedule the rewards (fireworks, sounds, commands) to run on the main server thread.
        foliaHelper.runTask(player, () -> {
            // Execute reward commands from the console.
            for (String command : achievement.commands()) {
                try {
                    String formattedCommand = command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                } catch (Exception e) {
                    logger.warning("Failed to execute achievement command: '" + command + "'. Error: " + e.getMessage());
                }
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            // Spawn a celebratory firework
            Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
            FireworkMeta fireworkMeta = firework.getFireworkMeta();
            fireworkMeta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.LIME, Color.YELLOW, Color.AQUA, Color.RED, Color.FUCHSIA)
                    .withFade(Color.WHITE)
                    .withFlicker()
                    .withTrail()
                    .build());
            fireworkMeta.setPower(0);
            firework.setFireworkMeta(fireworkMeta);

            // Send Message
            if (!achievement.message().isEmpty()) {
                String formattedMessage = ColorHelper.parse(achievement.message()).replace("{player}", player.getName());
                if (achievement.broadcast()) {
                    Bukkit.broadcastMessage(formattedMessage);
                } else {
                    messagesHelper.sendPlayerMessage(player, messagesHandler.getAchievementUnlockedMessage().replace("{0}", achievement.name));
                }
            }
        });
    }

    /**
     * Helper method to get the correct stat value from a PlayerStats object.
     */
    private double getStatValue(PlayerStats stats, StatType type) {
        return switch (type) {
            case TOTAL_DISTANCE -> stats.getTotalDistance();
            case LONGEST_FLIGHT -> stats.getLongestFlight();
            case TOTAL_FLIGHT_TIME -> stats.getTotalTimeSeconds();
            case BOOSTS_USED -> stats.getBoostsUsed();
            case SUPER_BOOSTS_USED -> stats.getSuperBoostsUsed();
            case SAVES -> stats.getPluginSaves();
            default -> 0.0;
        };
    }
}