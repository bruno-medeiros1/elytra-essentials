package org.bruno.elytraEssentials.handlers;

import com.google.common.base.Function;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import org.jetbrains.annotations.Nullable;

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

    private HikariDataSource dataSource;

    private enum StorageType { SQLITE, MYSQL }
    private StorageType storageType;
    private String prefix;

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private ConfigHandler.DatabaseOptions databaseOptions;

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
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database);
            config.setUsername(this.username);
            config.setPassword(this.password);

            config.setMaximumPoolSize(this.databaseOptions.maximumPoolSize());
            config.setMinimumIdle(this.databaseOptions.minimumIdle());
            config.setConnectionTimeout(this.databaseOptions.connectionTimeout());
            config.setIdleTimeout(600000);   // 10 minutes
            config.setKeepaliveTime(this.databaseOptions.keepaliveTime());
            config.setMaxLifetime(this.databaseOptions.maximumLifetime());
            config.setValidationTimeout(5000); // 5 seconds
            config.setConnectionTestQuery("SELECT 1");

            this.databaseOptions.properties().forEach(config::addDataSourceProperty);

            this.dataSource = new HikariDataSource(config);
        } else { // SQLITE
            File databaseFolder = new File(plugin.getDataFolder(), Constants.Files.DB_FOLDER);
            if (!databaseFolder.exists()) {
                if (!databaseFolder.mkdirs()) {
                    throw new SQLException("FATAL: Failed to create database folder. Please check file system permissions!");
                }
            }
            File dbFile = new File(databaseFolder, Constants.Files.SQLITE_DB_NAME);
            HikariConfig config = new HikariConfig();
            config.setDataSourceClassName("org.sqlite.SQLiteDataSource");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

            dataSource = new HikariDataSource(config);
        }
        logger.info("Database connection established.");
        initializeTables();

        // Run migration if old tables exist
        migrateOldTables();
    }

    public boolean isConnected() {
        return this.dataSource.isRunning();
    }

    public void disconnect() {
        if (isConnected()) {
            this.dataSource.close();
            messagesHelper.sendDebugMessage(storageType.name() + " database connection closed successfully!");
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

        this.prefix = configHandler.getPrefix();

        if (this.storageType == StorageType.MYSQL) {
            this.host = configHandler.getHost();
            this.port = configHandler.getPort();
            this.database = configHandler.getDatabase();
            this.username = configHandler.getUsername();
            this.password = configHandler.getPassword();
            this.databaseOptions = configHandler.getDataBaseOptions();
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
     * @param sender         The CommandSender who initiated the import.
     * @param backupFile     The actual File object representing the backup.
     */
    public void importFromBackup(String backupFileName, CommandSender sender, File backupFile) {
        logger.warning("Starting database import from backup: " + backupFileName);
        messagesHelper.sendCommandSenderMessage(sender, "&eStarting import process... Kicking all players.");

        // This all happens synchronously on the main thread to avoid data corruption.
        try {
            // 1. Kick players & shutdown services
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer("Server is restoring a data backup. Please reconnect in a moment.");
            }

            logger.info("All players kicked. Shutting down plugin services...");
            plugin.shutdownAllPluginTasks();
            this.disconnect();

            // 2. Perform file operations
            File databaseFolder = new File(plugin.getDataFolder(), Constants.Files.DB_FOLDER);
            File liveDbFile = new File(databaseFolder, Constants.Files.SQLITE_DB_NAME);
            File tempDbFile = new File(databaseFolder, "import_temp.db");

            Files.copy(backupFile.toPath(), tempDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 3. Verify the backup
            try (Connection testConnection = DriverManager.getConnection("jdbc:sqlite:" + tempDbFile.getAbsolutePath())) {
                if (!testConnection.isValid(2)) {
                    throw new SQLException("Import verification failed: Backup file is corrupted.");
                }
                logger.info("Backup file verified successfully.");
            } catch (SQLException e) {
                // If verification fails, clean up and restart services with the OLD database.
                messagesHelper.sendCommandSenderMessage(sender, "&cImport failed! The backup file appears corrupted. No changes were made.");
                logger.log(Level.SEVERE, "Could not verify backup file '" + backupFileName + "'. Aborting.", e);
                if (!tempDbFile.delete()) logger.warning("Could not delete temporary DB file.");
                this.initialize();
                plugin.startAllPluginTasks();
                return;
            }

            // 4. Commit the change
            if (liveDbFile.exists() && !liveDbFile.delete())
                throw new IOException("Could not delete old live DB file.");
            if (!tempDbFile.renameTo(liveDbFile)) throw new IOException("Could not rename temp DB file.");

            logger.info("Successfully restored backup. Re-initializing services...");
            this.initialize();
            plugin.startAllPluginTasks();

            messagesHelper.sendCommandSenderMessage(sender, "&aDatabase import successful! Players can now reconnect.");
            logger.info("Database import complete.");

        } catch (Exception e) {
            messagesHelper.sendCommandSenderMessage(sender, "&cA critical error occurred during import. Check console. The server may need a restart.");
            logger.log(Level.SEVERE, "Failed to perform database import.", e);
            // Attempt to restart services in a failed state
            try {
                this.initialize();
                plugin.startAllPluginTasks();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Failed to re-initialize services after a failed import.", ex);
            }
        }
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
        String tableName = applyPrefix(Constants.Database.Tables.ELYTRA_FLIGHT_TIME);
        String query = "SELECT flight_time FROM " + tableName + " WHERE uuid = ?";

        return this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("flight_time");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return 0;
        });
    }

    public void setPlayerFlightTime(UUID uuid, int time) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.ELYTRA_FLIGHT_TIME);
        String query;

        if (storageType == StorageType.MYSQL) {
            query = "INSERT INTO " + tableName + " (uuid, flight_time) VALUES (?, ?) ON DUPLICATE KEY UPDATE flight_time = ?";
        } else {
            query = "REPLACE INTO " + tableName + " (uuid, flight_time) VALUES (?, ?)";
        }

        this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, time);

                if (storageType == StorageType.MYSQL) {
                    stmt.setInt(3, time); // For ON DUPLICATE KEY UPDATE
                }

                stmt.executeUpdate();
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void addOwnedEffect(UUID playerUuid, String effectKey) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.OWNED_EFFECTS);
        String query;

        if (storageType == StorageType.MYSQL) {
            query = "INSERT IGNORE INTO " + tableName + " (player_uuid, effect_key, is_active) VALUES (?, ?, ?)";
        } else {
            query = "INSERT OR IGNORE INTO " + tableName + " (player_uuid, effect_key, is_active) VALUES (?, ?, ?)";
        }

        this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, effectKey);
                stmt.setBoolean(3, false); // New effects are inactive by default
                stmt.executeUpdate();
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * Removes a specific owned effect from a player.
     *
     * @param playerUuid The UUID of the player.
     * @param effectKey  The key of the effect to remove.
     * @throws SQLException If a database error occurs.
     */
    public void removeOwnedEffect(UUID playerUuid, String effectKey) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.OWNED_EFFECTS);
        String query = "DELETE FROM " + tableName + " WHERE player_uuid = ? AND effect_key = ?";

        this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, effectKey);
                stmt.executeUpdate();
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void updateOwnedEffect(UUID playerId, String effectKey, boolean isActive) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.OWNED_EFFECTS);
        String query = "UPDATE " + tableName + " SET is_active = ? WHERE player_uuid = ? AND effect_key = ?";

        this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setBoolean(1, isActive);
                stmt.setString(2, playerId.toString());
                stmt.setString(3, effectKey);
                stmt.executeUpdate();
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<String> getOwnedEffectKeys(UUID playerId) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.OWNED_EFFECTS);
        String query = "SELECT effect_key FROM " + tableName + " WHERE player_uuid = ?";

        return this.withConnection((connection) -> {
            List<String> ownedEffects = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ownedEffects.add(rs.getString(Constants.NBT.EFFECT_KEY));
                    }
                }
                return ownedEffects;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Nullable
    public String getPlayerActiveEffect(UUID playerId) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.OWNED_EFFECTS);
        String query = "SELECT effect_key FROM " + tableName + " WHERE player_uuid = ? AND is_active = 1";

        return this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(Constants.NBT.EFFECT_KEY);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
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
        String tableName = applyPrefix(Constants.Database.Tables.PLAYER_STATS);
        String query = "SELECT * FROM " + tableName + " WHERE uuid = ?";

        return this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Player found — load stats
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
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            // Player not found — return default stats
            return new PlayerStats(uuid);
        });
    }

    /**
     * Resets all statistics for a given player back to their default zero values
     * in both the stats and flight timetables.
     *
     * @param uuid The UUID of the player to reset.
     * @throws SQLException If a database error occurs.
     */
    public void resetPlayerStats(UUID uuid) throws SQLException {
        String playerStatsTable = applyPrefix(Constants.Database.Tables.PLAYER_STATS);
        String flightTimeTable = applyPrefix(Constants.Database.Tables.ELYTRA_FLIGHT_TIME);

        String resetStatsQuery = "UPDATE " + playerStatsTable + " SET " +
                "total_distance = 0, " +
                "total_time_seconds = 0, " +
                "longest_flight = 0, " +
                "boosts_used = 0, " +
                "super_boosts_used = 0, " +
                "plugin_saves = 0 " +
                "WHERE uuid = ?";

        String resetFlightTimeQuery = "UPDATE " + flightTimeTable + " SET " +
                "flight_time = 0 " +
                "WHERE uuid = ?";

        this.withConnection((connection) -> {
            // Reset Player Stats Table
            try (PreparedStatement stmt = connection.prepareStatement(resetStatsQuery)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            // Reset Flight Time Table
            try (PreparedStatement stmt = connection.prepareStatement(resetFlightTimeQuery)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return null;
        });
    }

    /**
     * Retrieves the top N players for a specific statistic from the database.
     *
     * @param statColumn The name of the database column to order by (e.g., "total_distance").
     * @param limit      The number of top players to retrieve.
     * @return A LinkedHashMap of Player UUIDs to their scores, sorted in descending order.
     * @throws SQLException If a database error occurs.
     */
    public Map<UUID, Double> getTopStats(String statColumn, int limit) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.PLAYER_STATS);

        // Query selects top players, ordered by the given stat column
        String query = "SELECT uuid, " + statColumn + " FROM " + tableName + " ORDER BY " + statColumn + " DESC LIMIT ?";

        return this.withConnection((connection) -> {
            Map<UUID, Double> topStats = new LinkedHashMap<>();
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        double value = rs.getDouble(statColumn);
                        topStats.put(uuid, value);
                    }
                }
                return topStats;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Calculates a player's rank for a specific statistic.
     *
     * @param uuid       The UUID of the player.
     * @param statColumn The database column for the statistic.
     * @return The player's rank, or -1 if not ranked.
     * @throws SQLException If a database error occurs.
     */
    public int getPlayerRank(UUID uuid, String statColumn) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.PLAYER_STATS);

        // Count how many players have a higher score than the target player
        String query = "SELECT COUNT(*) FROM " + tableName +
                " WHERE " + statColumn + " > (SELECT " + statColumn + " FROM " + tableName + " WHERE uuid = ?)";

        return this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) + 1; // Rank is 1 + number of players better
                    }
                }
                return -1; // Player not found or error
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * Saves a player's complete statistics to the database.
     * This will create a new row if one doesn't exist, or update the existing one.
     *
     * @param stats The PlayerStats object to save.
     * @throws SQLException If a database error occurs.
     */
    public void savePlayerStats(PlayerStats stats) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.PLAYER_STATS);
        String query;

        if (storageType == StorageType.MYSQL) {
            query = "INSERT INTO " + tableName + " (uuid, total_distance, total_time_seconds, longest_flight, boosts_used, super_boosts_used, plugin_saves) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "total_distance = ?, total_time_seconds = ?, longest_flight = ?, boosts_used = ?, super_boosts_used = ?, plugin_saves = ?";
        } else {
            query = "REPLACE INTO " + tableName + " (uuid, total_distance, total_time_seconds, longest_flight, boosts_used, super_boosts_used, plugin_saves) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                // Set values for INSERT
                stmt.setString(1, stats.getUuid().toString());
                stmt.setDouble(2, stats.getTotalDistance());
                stmt.setLong(3, stats.getTotalTimeSeconds());
                stmt.setDouble(4, stats.getLongestFlight());
                stmt.setInt(5, stats.getBoostsUsed());
                stmt.setInt(6, stats.getSuperBoostsUsed());
                stmt.setInt(7, stats.getPluginSaves());

                // If MySQL, set values again for the UPDATE part
                if (storageType == StorageType.MYSQL) {
                    stmt.setDouble(8, stats.getTotalDistance());
                    stmt.setLong(9, stats.getTotalTimeSeconds());
                    stmt.setDouble(10, stats.getLongestFlight());
                    stmt.setInt(11, stats.getBoostsUsed());
                    stmt.setInt(12, stats.getSuperBoostsUsed());
                    stmt.setInt(13, stats.getPluginSaves());
                }

                stmt.executeUpdate();
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

//<editor-fold desc="ACHIEVEMENTS">

    /**
     * Retrieves a set of all achievement IDs that a player has unlocked.
     *
     * @param playerUuid The UUID of the player.
     * @return A Set of achievement ID strings.
     * @throws SQLException If a database error occurs.
     */
    public Set<String> getUnlockedAchievementIds(UUID playerUuid) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.PLAYER_ACHIEVEMENTS);

        String query = "SELECT achievement_id FROM " + tableName + " WHERE player_uuid = ?";

        return this.withConnection((connection) -> {
            Set<String> unlockedIds = new HashSet<>();
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        unlockedIds.add(rs.getString("achievement_id"));
                    }
                }
                return unlockedIds;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Checks if a player has already unlocked a specific achievement.
     *
     * @param playerUuid    The UUID of the player.
     * @param achievementId The unique ID of the achievement.
     * @return true if the player has the achievement, false otherwise.
     * @throws SQLException If a database error occurs.
     */
    public boolean hasAchievement(UUID playerUuid, String achievementId) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.PLAYER_ACHIEVEMENTS);

        String query = "SELECT 1 FROM " + tableName + " WHERE player_uuid = ? AND achievement_id = ?";

        return this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, achievementId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next(); // True if a row exists
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Adds a completed achievement record for a player to the database.
     *
     * @param playerUuid    The UUID of the player.
     * @param achievementId The unique ID of the achievement.
     * @throws SQLException If a database error occurs.
     */
    public void addAchievement(UUID playerUuid, String achievementId) throws SQLException {
        String tableName = applyPrefix(Constants.Database.Tables.PLAYER_ACHIEVEMENTS);

        String query = "INSERT INTO " + tableName + " (player_uuid, achievement_id) VALUES (?, ?)";

        // Check if the achievement already exists
        if (hasAchievement(playerUuid, achievementId)) {
            return;
        }

        this.withConnection((connection) -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, achievementId);
                stmt.executeUpdate();
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

//</editor-fold>

    private void migrateOldTables() throws SQLException {
        migrateIfNeeded(Constants.Database.Tables.ELYTRA_FLIGHT_TIME, applyPrefix(Constants.Database.Tables.ELYTRA_FLIGHT_TIME));
        migrateIfNeeded(Constants.Database.Tables.OWNED_EFFECTS, applyPrefix(Constants.Database.Tables.OWNED_EFFECTS));
        migrateIfNeeded(Constants.Database.Tables.PLAYER_STATS, applyPrefix(Constants.Database.Tables.PLAYER_STATS));
        migrateIfNeeded(Constants.Database.Tables.PLAYER_ACHIEVEMENTS, applyPrefix(Constants.Database.Tables.PLAYER_ACHIEVEMENTS));
    }

    private void migrateIfNeeded(String oldTable, String newTable) throws SQLException {
        if (tableExists(oldTable) && !tableExists(newTable)) {
            logger.info("Migrating data from old table: " + oldTable + " → " + newTable);

            // Ensure new table exists
            executeTableQuery(newTable);

            // Copy data
            this.withConnection((connection) -> {
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("INSERT INTO " + newTable + " SELECT * FROM " + oldTable + ";");
                    return null;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            logger.info("Migration complete for table: " + oldTable);
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        return this.withConnection((connection) -> {
            try {
                DatabaseMetaData meta = connection.getMetaData();
                try (ResultSet rs = meta.getTables(null, null, tableName, null)) {
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void initializeTables() throws SQLException {
        executeTableQuery(Constants.Database.Tables.ELYTRA_FLIGHT_TIME);
        executeTableQuery(Constants.Database.Tables.OWNED_EFFECTS);
        executeTableQuery(Constants.Database.Tables.PLAYER_STATS);
        executeTableQuery(Constants.Database.Tables.PLAYER_ACHIEVEMENTS);
        logger.info("Database tables verified and initialized successfully.");
    }

    private void executeTableQuery(String tableName) throws SQLException {
        String query = getCreateTableQuery(applyPrefix(tableName));
        if (query == null) {
            plugin.getLogger().warning("A database setup issue was detected with table: " + tableName);
            return;
        }
        this.withConnection((connection) -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(query);
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String getCreateTableQuery(String tableName) {
        boolean isMysql = storageType == StorageType.MYSQL;

        String lower = tableName.toLowerCase();

        if (lower.contains(Constants.Database.Tables.ELYTRA_FLIGHT_TIME)) {
            return isMysql ?
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (uuid VARCHAR(36) PRIMARY KEY, flight_time INT DEFAULT 0);" :
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (uuid TEXT PRIMARY KEY, flight_time INTEGER DEFAULT 0);";
        }

        if (lower.contains(Constants.Database.Tables.OWNED_EFFECTS)) {
            return isMysql ?
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, effect_key VARCHAR(255) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT FALSE, owned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP);" :
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, effect_key TEXT NOT NULL, is_active INTEGER NOT NULL DEFAULT 0, owned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
        }

        if (lower.contains(Constants.Database.Tables.PLAYER_STATS)) {
            return isMysql ?
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (uuid VARCHAR(36) PRIMARY KEY, total_distance DOUBLE DEFAULT 0, total_time_seconds BIGINT DEFAULT 0, longest_flight DOUBLE DEFAULT 0, boosts_used INT DEFAULT 0, super_boosts_used INT DEFAULT 0, plugin_saves INT DEFAULT 0);" :
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (uuid TEXT PRIMARY KEY, total_distance REAL DEFAULT 0, total_time_seconds INTEGER DEFAULT 0, longest_flight REAL DEFAULT 0, boosts_used INTEGER DEFAULT 0, super_boosts_used INTEGER DEFAULT 0, plugin_saves INTEGER DEFAULT 0);";
        }

        if (lower.contains(Constants.Database.Tables.PLAYER_ACHIEVEMENTS)) {
            return isMysql ?
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (player_uuid VARCHAR(36) NOT NULL, achievement_id VARCHAR(255) NOT NULL, unlocked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (player_uuid, achievement_id));" :
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (player_uuid TEXT NOT NULL, achievement_id TEXT NOT NULL, unlocked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (player_uuid, achievement_id));";
        }

        return null;
    }

    private String applyPrefix(String tableName) {
        return prefix + tableName;
    }

    private <T> T withConnection(Function<Connection, T> action) throws SQLException {
        Connection conn = null;
        try {
            conn = this.dataSource.getConnection();
            return action.apply(conn);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}