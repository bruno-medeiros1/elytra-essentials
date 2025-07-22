package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseHandler {
    private final ElytraEssentials plugin;
    private final ConfigHandler configHandler;
    private final FoliaHelper foliaHelper;
    private final MessagesHelper messagesHelper;
    private final Logger logger;

    private Connection connection;

    private enum StorageType { SQLITE, MYSQL }
    private StorageType storageType;

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    private CancellableTask backupTask = null;

    public DatabaseHandler(ElytraEssentials plugin, ConfigHandler configHandler, FoliaHelper foliaHelper, MessagesHelper messagesHelper, Logger logger) {
        this.plugin = plugin;
        this.configHandler = configHandler;
        this.foliaHelper = foliaHelper;
        this.messagesHelper = messagesHelper;
        this.logger = logger;

        setDatabaseVariables();
    }

    public void initialize() throws SQLException {
        logger.info("Using " + storageType.name() + " for data storage.");

        if (storageType == StorageType.MYSQL) {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false",
                    this.username, this.password
            );
        } else { // SQLITE
            File databaseFolder = new File(plugin.getDataFolder(), Constants.Files.DB_FOLDER);
            if (!databaseFolder.exists()) {
                if (!databaseFolder.mkdirs()) {
                    throw new SQLException("FATAL: Failed to create database folder. Please check file system permissions!");
                }
            }
            File dbFile = new File(databaseFolder, Constants.Files.SQLITE_DB_NAME);
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
        logger.info("Database connection established.");
        initializeTables();
    }

    public boolean isConnected() {
        return connection != null;
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                connection.close();
                messagesHelper.sendDebugMessage(storageType.name() + " database connection closed successfully!");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to close the database connection.", e);
            }
        }
    }

    public void setDatabaseVariables() {
        String typeFromConfig = configHandler.getStorageType().toUpperCase();
        try {
            this.storageType = StorageType.valueOf(typeFromConfig);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid storage type '" + typeFromConfig + "' in config.yml. Defaulting to SQLITE.");
            this.storageType = StorageType.SQLITE;
        }

        if (this.storageType == StorageType.MYSQL) {
            this.host = configHandler.getHost();
            this.port = configHandler.getPort();
            this.database = configHandler.getDatabase();
            this.username = configHandler.getUsername();
            this.password = configHandler.getPassword();
        }
    }

    public String getStorageType() {
        return (this.storageType != null) ? this.storageType.name() : "SQLITE";
    }

    //<editor-fold desc="BACKUPS">
    public void start() {
        if (storageType != StorageType.SQLITE) return;

        if (!configHandler.getIsAutoBackupEnabled()) return;

        if (this.backupTask != null) return;

        long interval = TimeHelper.minutesToTicks(configHandler.getAutoBackupInterval());

        logger.info("Starting automatic database backup task...");

        // This is the new, platform-safe way to schedule the repeating backup.
        this.backupTask = foliaHelper.runTaskTimerGlobal(() -> {
            foliaHelper.runAsyncTask(this::backupSQLiteDatabase);
        }, interval, interval);
    }

    public void shutdown() {
        if (this.backupTask != null) {
            this.backupTask.cancel();
            this.backupTask = null;
        }
    }

    /**
     * Imports a database backup file into the live database.
     * This method will kick all players, copy the backup to a temporary file,
     * verify its integrity, and then swap it with the live database.
     *
     * @param backupFileName The name of the backup file to import.
     * @param sender The CommandSender who initiated the import.
     * @param backupFile The actual File object representing the backup.
     */
    public void importFromBackup(String backupFileName, CommandSender sender, File backupFile) {
        logger.warning("Starting database import from backup: " + backupFileName);
        messagesHelper.sendCommandSenderMessage(sender, "&eStarting import process... Kicking all players.");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer("Server is restoring a data backup. Please reconnect in a moment.");
        }

        logger.info("All players kicked. Shutting down plugin services...");
        plugin.shutdownAllPluginTasks();
        this.disconnect();

        foliaHelper.runAsyncTask(() -> {
            File databaseFolder = new File(plugin.getDataFolder(), Constants.Files.DB_FOLDER);
            File liveDbFile = new File(databaseFolder, Constants.Files.SQLITE_DB_NAME);
            File tempDbFile = new File(databaseFolder, "import_temp.db");

            try {
                // 1. STAGING
                Files.copy(backupFile.toPath(), tempDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // 2. VERIFICATION
                try (Connection testConnection = DriverManager.getConnection("jdbc:sqlite:" + tempDbFile.getAbsolutePath())) {
                    if (!testConnection.isValid(2)) {
                        throw new SQLException("Import verification failed: Backup file is corrupted or invalid.");
                    }
                    logger.info("Backup file verified successfully.");
                } catch (SQLException e) {
                    // If verification fails, schedule cleanup and restart on the main thread
                    foliaHelper.runTaskOnMainThread(() -> {
                        messagesHelper.sendCommandSenderMessage(sender,"&cImport failed! The selected backup file appears to be corrupted. No changes were made.");
                        logger.log(Level.SEVERE, "Could not verify the integrity of the backup file '" + backupFileName + "'. Aborting import.", e);
                        if (!tempDbFile.delete()) {
                            logger.warning("Could not delete temporary database file: " + tempDbFile.getName());
                        }
                        try {
                            this.initialize(); // Reconnect to the original database
                            plugin.startAllPluginTasks();
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, "Failed to reconnect to original database after failed import.", ex);
                        }
                    });
                    return; // Stop the async task
                }

                // 3. COMMIT
                if (liveDbFile.exists() && !liveDbFile.delete()) throw new IOException("Could not delete the old live database file.");
                if (!tempDbFile.renameTo(liveDbFile)) throw new IOException("Could not rename temporary database file.");

                logger.info("Successfully restored backup file to live database.");

                // 4. RE-INITIALIZE (on the main thread)
                foliaHelper.runTaskOnMainThread(() -> {
                    try {
                        logger.info("Re-initializing plugin services...");
                        this.initialize();
                        plugin.startAllPluginTasks();
                        messagesHelper.sendCommandSenderMessage(sender, "&aDatabase import successful! Players can now reconnect.");
                        logger.info("Database import complete.");
                    } catch (SQLException e) {
                        messagesHelper.sendCommandSenderMessage(sender,"&cImport failed during re-initialization. The server may need a restart. Check console.");
                        logger.log(Level.SEVERE, "Failed to re-initialize services after database import.", e);
                    }
                });

            } catch (IOException e) {
                foliaHelper.runTaskOnMainThread(() -> {
                    messagesHelper.sendCommandSenderMessage(sender,"&cA critical file error occurred. Check the console for details.");
                    logger.log(Level.SEVERE, "Failed to perform database file swap during import.", e);
                });
            }
        });
    }

    public List<String> getBackupFileNames() {
        if (storageType != StorageType.SQLITE) {
            return Collections.emptyList();
        }
        List<String> fileNames = new ArrayList<>();
        File backupFolder = new File(plugin.getDataFolder(), Constants.Files.DB_FOLDER + "/" + Constants.Files.DB_BACKUP_FOLDER);
        if (backupFolder.exists() && backupFolder.isDirectory()) {
            File[] files = backupFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".db"));
            if (files != null) {
                for (File file : files) {
                    fileNames.add(file.getName());
                }
            }
        }
        return fileNames;
    }

    private void backupSQLiteDatabase() {
        File databaseFolder = new File(plugin.getDataFolder(), Constants.Files.DB_FOLDER);
        File sourceFile = new File(databaseFolder, Constants.Files.SQLITE_DB_NAME);

        if (!sourceFile.exists()) {
            logger.warning("SQLite database file not found. Skipping backup.");
            return;
        }

        File backupFolder = new File(databaseFolder, Constants.Files.DB_BACKUP_FOLDER);
        if (!backupFolder.exists()) {
            if (!backupFolder.mkdirs()) {
                logger.severe("Could not create backups folder! Please check file system permissions. Skipping backup.");
                return;
            }
        }

        File[] backupFiles = backupFolder.listFiles((dir, name) -> name.endsWith(".db"));
        if (backupFiles != null && backupFiles.length >= configHandler.getAutoBackupMaxBackups()) {
            Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));
            File oldestFile = backupFiles[0];
            if (oldestFile.delete()) {
                messagesHelper.sendDebugMessage("Deleted oldest backup file: " + oldestFile.getName());
            } else {
                logger.warning("Could not delete oldest backup file: " + oldestFile.getName());
            }
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String formattedDate = formatter.format(new Date());
        String backupFileName = "backup_" + formattedDate + ".db";
        File destinationFile = new File(backupFolder, backupFileName);

        try {
            Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            messagesHelper.sendDebugMessage("Successfully created database backup: " + destinationFile.getName());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not create SQLite database backup! Check file permissions.", e);
        }
    }

    //</editor-fold>

    public int getPlayerFlightTime(UUID uuid) throws SQLException {
        String query = "SELECT flight_time FROM " + Constants.Database.Tables.ELYTRA_FLIGHT_TIME + " WHERE uuid = ?";

        // Wrap in the conditional block for structural consistency.
        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("flight_time");
                    }
                }
            }
        }

        // Default to 0 if no record is found for the player.
        return 0;
    }


    public void setPlayerFlightTime(UUID uuid, int time) throws SQLException {
        String query;
        if (storageType == StorageType.MYSQL) {
            query = "INSERT INTO " + Constants.Database.Tables.ELYTRA_FLIGHT_TIME + " (uuid, flight_time) VALUES (?, ?) ON DUPLICATE KEY UPDATE flight_time = ?";
        } else {
            query = "REPLACE INTO " + Constants.Database.Tables.ELYTRA_FLIGHT_TIME + " (uuid, flight_time) VALUES (?, ?)";
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, time);

            // For the MySQL query, we need to set the third parameter for the UPDATE part
            if (storageType == StorageType.MYSQL) {
                stmt.setInt(3, time);
            }

            stmt.executeUpdate();
        }
    }

    public void addOwnedEffect(UUID playerUuid, String effectKey) throws SQLException {
        String query;
        if (storageType == StorageType.MYSQL) {
            query = "INSERT IGNORE INTO " + Constants.Database.Tables.OWNED_EFFECTS + " (player_uuid, effect_key, is_active) VALUES (?, ?, ?)";
        } else {
            query = "INSERT OR IGNORE INTO " + Constants.Database.Tables.OWNED_EFFECTS + " (player_uuid, effect_key, is_active) VALUES (?, ?, ?)";
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, effectKey);
            stmt.setBoolean(3, false); // A newly added effect should never be active by default
            stmt.executeUpdate();
        }
    }

    /**
     * Removes a specific owned effect from a player.
     * @param playerUuid The UUID of the player.
     * @param effectKey The key of the effect to remove.
     * @throws SQLException If a database error occurs.
     */
    public void removeOwnedEffect(UUID playerUuid, String effectKey) throws SQLException {
        String query = "DELETE FROM " + Constants.Database.Tables.OWNED_EFFECTS + " WHERE player_uuid = ? AND effect_key = ?";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, effectKey);
                stmt.executeUpdate();
            }
        }
    }

    public void updateOwnedEffect(UUID playerId, String effectKey, boolean isActive) throws SQLException {
        String query = "UPDATE " + Constants.Database.Tables.OWNED_EFFECTS + " SET is_active = ? WHERE player_uuid = ? AND effect_key = ?";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setBoolean(1, isActive);
                stmt.setString(2, playerId.toString());
                stmt.setString(3, effectKey);
                stmt.executeUpdate();
            }
        }
    }


    public boolean getIsActiveOwnedEffect(UUID playerId, String effectKey) throws SQLException {
        String query = "SELECT is_active FROM " + Constants.Database.Tables.OWNED_EFFECTS + " WHERE player_uuid = ? AND effect_key = ?";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, effectKey);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("is_active");
                    }
                }
            }
        }

        // If no record is found, throw an exception as per the original logic.
        throw new SQLException("No matching effect record found for player " + playerId + " with key " + effectKey);
    }


    public List<String> getOwnedEffectKeys(UUID playerId) throws SQLException {
        List<String> ownedEffects = new ArrayList<>();
        String query = "SELECT effect_key FROM " + Constants.Database.Tables.OWNED_EFFECTS + " WHERE player_uuid = ?";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ownedEffects.add(rs.getString(Constants.NBT.EFFECT_KEY));
                    }
                }
            }
        }

        return ownedEffects;
    }


    public String getPlayerActiveEffect(UUID playerId) throws SQLException {
        String query = "SELECT effect_key FROM " + Constants.Database.Tables.OWNED_EFFECTS + " WHERE player_uuid = ? AND is_active = 1";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(Constants.NBT.EFFECT_KEY);
                    }
                }
            }
        }
        return null;
    }


    /**
     * Retrieves all statistics for a given player from the database.
     * If the player has no stats entry, a new default stats object is returned.
     *
     * @param uuid The UUID of the player.
     * @return A PlayerStats object containing all their stats.
     * @throws SQLException If a database error occurs.
     */
    public PlayerStats getPlayerStats(UUID uuid) throws SQLException {
        String query = "SELECT * FROM " + Constants.Database.Tables.PLAYER_STATS + " WHERE uuid = ?";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Player was found in the database, so we load their data.
                        PlayerStats stats = new PlayerStats(uuid);
                        stats.setTotalDistance(rs.getDouble("total_distance"));
                        stats.setTotalTimeSeconds(rs.getLong("total_time_seconds"));
                        stats.setLongestFlight(rs.getDouble("longest_flight"));
                        stats.setBoostsUsed(rs.getInt("boosts_used"));
                        stats.setSuperBoostsUsed(rs.getInt("super_boosts_used"));
                        stats.setPluginSaves(rs.getInt("plugin_saves"));
                        return stats;
                    }
                }
            }
        }

        // If no entry was found in the database for the player,
        // we return a new, clean PlayerStats object with all values at 0.
        return new PlayerStats(uuid);
    }


    /**
     * Resets all statistics for a given player back to their default zero values
     * in both the stats and flight time tables.
     * @param uuid The UUID of the player to reset.
     * @throws SQLException If a database error occurs.
     */
    public void resetPlayerStats(UUID uuid) throws SQLException {
        String resetStatsQuery = "UPDATE " + Constants.Database.Tables.PLAYER_STATS + " SET " +
                "total_distance = 0, " +
                "total_time_seconds = 0, " +
                "longest_flight = 0, " +
                "boosts_used = 0, " +
                "super_boosts_used = 0, " +
                "plugin_saves = 0 " +
                "WHERE uuid = ?";

        String resetFlightTimeQuery = "UPDATE " + Constants.Database.Tables.ELYTRA_FLIGHT_TIME + " SET " +
                "flight_time = 0 " +
                "WHERE uuid = ?";

        // Although the UPDATE syntax is the same for both, we use the if/else
        // block to maintain our code structure and for future-proofing.
        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            // Reset Player Stats Table
            try (PreparedStatement stmt = connection.prepareStatement(resetStatsQuery)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }

            // Reset Flight Time Table
            try (PreparedStatement stmt = connection.prepareStatement(resetFlightTimeQuery)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
        }
    }


    /**
     * Retrieves the top N players for a specific statistic from the database.
     *
     * @param statColumn The name of the database column to order by (e.g., "total_distance").
     * @param limit The number of top players to retrieve.
     * @return A LinkedHashMap of Player UUIDs to their scores, sorted in descending order.
     * @throws SQLException If a database error occurs.
     */
    public Map<UUID, Double> getTopStats(String statColumn, int limit) throws SQLException {
        // A LinkedHashMap preserves the insertion order, which is perfect for a sorted leaderboard.
        Map<UUID, Double> topStats = new LinkedHashMap<>();

        // This query selects the top players, ordering them by the specified column.
        String query = "SELECT uuid, " + statColumn + " FROM " + Constants.Database.Tables.PLAYER_STATS + " ORDER BY " + statColumn + " DESC LIMIT ?";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, limit); // Set the LIMIT value
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        double value = rs.getDouble(statColumn);
                        topStats.put(uuid, value);
                    }
                }
            }
        }
        return topStats;
    }

    /**
     * Calculates a player's rank for a specific statistic.
     * @param uuid The UUID of the player.
     * @param statColumn The database column for the statistic.
     * @return The player's rank, or -1 if not ranked.
     * @throws SQLException If a database error occurs.
     */
    public int getPlayerRank(UUID uuid, String statColumn) throws SQLException {
        // This query counts how many players have a better score than the target player.
        // Adding 1 gives us the player's rank (e.g., if 0 players are better, rank is #1).
        String query = "SELECT COUNT(*) FROM " + Constants.Database.Tables.PLAYER_STATS +
                " WHERE " + statColumn + " > (SELECT " + statColumn + " FROM " +
                Constants.Database.Tables.PLAYER_STATS + " WHERE uuid = ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) + 1;
                }
            }
        }
        return -1; // Indicates player not found or an error
    }

    /**
     * Saves a player's complete statistics to the database.
     * This will create a new row if one doesn't exist, or update the existing one.
     * @param stats The PlayerStats object to save.
     * @throws SQLException If a database error occurs.
     */
    public void savePlayerStats(PlayerStats stats) throws SQLException {
        String query;
        if (storageType == StorageType.MYSQL) {
            query = "INSERT INTO " + Constants.Database.Tables.PLAYER_STATS + " (uuid, total_distance, total_time_seconds, longest_flight, boosts_used, super_boosts_used, plugin_saves) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "total_distance = ?, total_time_seconds = ?, longest_flight = ?, boosts_used = ?, super_boosts_used = ?, plugin_saves = ?";
        } else {
            query = "REPLACE INTO " + Constants.Database.Tables.PLAYER_STATS + " (uuid, total_distance, total_time_seconds, longest_flight, boosts_used, super_boosts_used, plugin_saves) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            // Set values for the query
            stmt.setString(1, stats.getUuid().toString());
            stmt.setDouble(2, stats.getTotalDistance());
            stmt.setLong(3, stats.getTotalTimeSeconds());
            stmt.setDouble(4, stats.getLongestFlight());
            stmt.setInt(5, stats.getBoostsUsed());
            stmt.setInt(6, stats.getSuperBoostsUsed());
            stmt.setInt(7, stats.getPluginSaves());

            // If it's MySQL, we need to set the values again for the UPDATE part
            if (storageType == StorageType.MYSQL) {
                stmt.setDouble(8, stats.getTotalDistance());
                stmt.setLong(9, stats.getTotalTimeSeconds());
                stmt.setDouble(10, stats.getLongestFlight());
                stmt.setInt(11, stats.getBoostsUsed());
                stmt.setInt(12, stats.getSuperBoostsUsed());
                stmt.setInt(13, stats.getPluginSaves());
            }

            stmt.executeUpdate();
        }
    }

    //<editor-fold desc="ACHIEVEMENTS">

    /**
     * Retrieves a set of all achievement IDs that a player has unlocked.
     * @param playerUuid The UUID of the player.
     * @return A Set of achievement ID strings.
     * @throws SQLException If a database error occurs.
     */
    public Set<String> getUnlockedAchievementIds(UUID playerUuid) throws SQLException {
        Set<String> unlockedIds = new HashSet<>();
        String query = "SELECT achievement_id FROM " + Constants.Database.Tables.PLAYER_ACHIEVEMENTS + " WHERE player_uuid = ?";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        unlockedIds.add(rs.getString("achievement_id"));
                    }
                }
            }
        }
        return unlockedIds;
    }

    /**
     * Checks if a player has already unlocked a specific achievement.
     * @param playerUuid The UUID of the player.
     * @param achievementId The unique ID of the achievement.
     * @return true if the player has the achievement, false otherwise.
     * @throws SQLException If a database error occurs.
     */
    public boolean hasAchievement(UUID playerUuid, String achievementId) throws SQLException {
        String query = "SELECT 1 FROM " + Constants.Database.Tables.PLAYER_ACHIEVEMENTS + " WHERE player_uuid = ? AND achievement_id = ? LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, achievementId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Returns true if a row was found
            }
        }
    }

    /**
     * Adds a completed achievement record for a player to the database.
     * @param playerUuid The UUID of the player.
     * @param achievementId The unique ID of the achievement.
     * @throws SQLException If a database error occurs.
     */
    public void addAchievement(UUID playerUuid, String achievementId) throws SQLException {
        String query;
        if (storageType == StorageType.MYSQL) {
            query = "INSERT IGNORE INTO " + Constants.Database.Tables.PLAYER_ACHIEVEMENTS + " (player_uuid, achievement_id) VALUES (?, ?)";
        } else {
            query = "INSERT OR IGNORE INTO " + Constants.Database.Tables.PLAYER_ACHIEVEMENTS + " (player_uuid, achievement_id) VALUES (?, ?)";
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, achievementId);
            stmt.executeUpdate();
        }
    }

    //</editor-fold>

    private void initializeTables() throws SQLException {
        executeTableQuery(Constants.Database.Tables.ELYTRA_FLIGHT_TIME);
        executeTableQuery(Constants.Database.Tables.OWNED_EFFECTS);
        executeTableQuery(Constants.Database.Tables.PLAYER_STATS);
        executeTableQuery(Constants.Database.Tables.PLAYER_ACHIEVEMENTS);
        logger.info("Database tables verified and initialized successfully.");
    }

    private void executeTableQuery(String tableName) throws SQLException {
        String query = getCreateTableQuery(tableName);
        if (query == null) {
            logger.warning("No schema found for table: " + tableName);
            return;
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(query);
        }
    }

    private String getCreateTableQuery(String tableName) {
        boolean isMysql = storageType == StorageType.MYSQL;
        return switch (tableName) {
            case Constants.Database.Tables.ELYTRA_FLIGHT_TIME -> isMysql ?
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (uuid VARCHAR(36) PRIMARY KEY, flight_time INT DEFAULT 0);" :
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (uuid TEXT PRIMARY KEY, flight_time INTEGER DEFAULT 0);";
            case Constants.Database.Tables.OWNED_EFFECTS -> isMysql ?
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, effect_key VARCHAR(255) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT FALSE, owned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP);" :
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, effect_key TEXT NOT NULL, is_active INTEGER NOT NULL DEFAULT 0, owned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
            case Constants.Database.Tables.PLAYER_STATS -> isMysql ?
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (uuid VARCHAR(36) PRIMARY KEY, total_distance DOUBLE DEFAULT 0, total_time_seconds BIGINT DEFAULT 0, longest_flight DOUBLE DEFAULT 0, boosts_used INT DEFAULT 0, super_boosts_used INT DEFAULT 0, plugin_saves INT DEFAULT 0);" :
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (uuid TEXT PRIMARY KEY, total_distance REAL DEFAULT 0, total_time_seconds INTEGER DEFAULT 0, longest_flight REAL DEFAULT 0, boosts_used INTEGER DEFAULT 0, super_boosts_used INTEGER DEFAULT 0, plugin_saves INTEGER DEFAULT 0);";
            case Constants.Database.Tables.PLAYER_ACHIEVEMENTS -> isMysql ?
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (player_uuid VARCHAR(36) NOT NULL, achievement_id VARCHAR(255) NOT NULL, unlocked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (player_uuid, achievement_id));" :
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (player_uuid TEXT NOT NULL, achievement_id TEXT NOT NULL, unlocked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (player_uuid, achievement_id));";
            default -> null;
        };
    }
}