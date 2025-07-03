package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class DatabaseHandler {
    private final static String ELYTRA_FLIGHT_TIME_TABLE = "elytra_flight_time";
    private final static String OWNED_EFFECTS_TABLE = "owned_effects";
    private final static String PLAYER_STATS_TABLE = "player_stats";

    private final ElytraEssentials plugin;
    private Connection connection;

    private enum StorageType { SQLITE, MYSQL }

    private StorageType storageType;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    private BukkitTask backupTask = null;
    private static final int MAX_BACKUPS = 24;
    private static final long AUTO_BACKUPS_TIME = 20L * 60 * 60;

    private static final String SQLITE_DATABASE_NAME = "elytraessentials.db";

    public DatabaseHandler(ElytraEssentials plugin){
        this.plugin = plugin;

        setDatabaseVariables();
    }


    public void Initialize() throws SQLException {
        String typeFromConfig = plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();
        try {
            this.storageType = StorageType.valueOf(typeFromConfig);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid storage type '" + typeFromConfig + "' in config.yml. Defaulting to SQLITE.");
            this.storageType = StorageType.SQLITE;
        }

        if (storageType == StorageType.MYSQL) {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false",
                    this.username, this.password
            );
        } else {
            File databaseFolder = new File(plugin.getDataFolder(), "database");
            if (!databaseFolder.exists()) {
                boolean wasCreated = databaseFolder.mkdirs();
                if (!wasCreated) {
                    throw new SQLException("FATAL: Failed to create database folder. Please check file system permissions!");
                }
            }

            File dbFile = new File(databaseFolder, SQLITE_DATABASE_NAME);
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }

        InitializeTables(); // Create tables with the correct syntax
    }


    public boolean IsConnected() {
        return connection != null;
    }


    public void Disconnect() {
        if (IsConnected()) {
            try {
                connection.close();
                plugin.getMessagesHelper().sendDebugMessage(storageType.name() + " database was closed successfully!");
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Failed to close the connection to the database.");
                Bukkit.getLogger().severe("Error: " + e.getMessage());
            }
        }
    }


    public int GetPlayerFlightTime(UUID uuid) throws SQLException {
        String query = "SELECT flight_time FROM " + ELYTRA_FLIGHT_TIME_TABLE + " WHERE uuid = ?";

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


    public void SetPlayerFlightTime(UUID uuid, int time) throws SQLException {
        String query;
        if (storageType == StorageType.MYSQL) {
            query = "INSERT INTO " + ELYTRA_FLIGHT_TIME_TABLE + " (uuid, flight_time) VALUES (?, ?) ON DUPLICATE KEY UPDATE flight_time = ?";
        } else {
            query = "REPLACE INTO " + ELYTRA_FLIGHT_TIME_TABLE + " (uuid, flight_time) VALUES (?, ?)";
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


    public void AddOwnedEffect(UUID playerUuid, String effectKey) throws SQLException {
        String query;
        if (storageType == StorageType.MYSQL) {
            query = "INSERT IGNORE INTO " + OWNED_EFFECTS_TABLE + " (player_uuid, effect_key, is_active) VALUES (?, ?, ?)";
        } else {
            query = "INSERT OR IGNORE INTO " + OWNED_EFFECTS_TABLE + " (player_uuid, effect_key, is_active) VALUES (?, ?, ?)";
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, effectKey);
            stmt.setBoolean(3, false); // A newly added effect should never be active by default
            stmt.executeUpdate();
        }
    }


    public void UpdateOwnedEffect(UUID playerId, String effectKey, boolean isActive) throws SQLException {
        String query = "UPDATE " + OWNED_EFFECTS_TABLE + " SET is_active = ? WHERE player_uuid = ? AND effect_key = ?";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setBoolean(1, isActive);
                stmt.setString(2, playerId.toString());
                stmt.setString(3, effectKey);
                stmt.executeUpdate();
            }
        }
    }


    public boolean GetIsActiveOwnedEffect(UUID playerId, String effectKey) throws SQLException {
        String query = "SELECT is_active FROM " + OWNED_EFFECTS_TABLE + " WHERE player_uuid = ? AND effect_key = ?";

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


    public List<String> GetOwnedEffectKeys(UUID playerId) throws SQLException {
        List<String> ownedEffects = new ArrayList<>();
        String query = "SELECT effect_key FROM " + OWNED_EFFECTS_TABLE + " WHERE player_uuid = ?";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ownedEffects.add(rs.getString("effect_key"));
                    }
                }
            }
        }

        return ownedEffects;
    }


    public String getPlayerActiveEffect(UUID playerId) throws SQLException {
        String query = "SELECT effect_key FROM " + OWNED_EFFECTS_TABLE + " WHERE player_uuid = ? AND is_active = 1";

        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("effect_key");
                    }
                }
            }
        }

        // Return null if no active effect is found
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
        String query = "SELECT * FROM " + PLAYER_STATS_TABLE + " WHERE uuid = ?";

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
        String resetStatsQuery = "UPDATE " + PLAYER_STATS_TABLE + " SET " +
                "total_distance = 0, " +
                "total_time_seconds = 0, " +
                "longest_flight = 0, " +
                "boosts_used = 0, " +
                "super_boosts_used = 0, " +
                "plugin_saves = 0 " +
                "WHERE uuid = ?";

        String resetFlightTimeQuery = "UPDATE " + ELYTRA_FLIGHT_TIME_TABLE + " SET " +
                "flight_time = 0 " +
                "WHERE uuid = ?";

        // Although the UPDATE syntax is the same for both, we use the if/else
        // block to maintain our code structure and for future-proofing.
        if (storageType == StorageType.MYSQL || storageType == StorageType.SQLITE) {
            // --- Reset Player Stats Table ---
            try (PreparedStatement stmt = connection.prepareStatement(resetStatsQuery)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }

            // --- Reset Flight Time Table ---
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
        String query = "SELECT uuid, " + statColumn + " FROM " + PLAYER_STATS_TABLE + " ORDER BY " + statColumn + " DESC LIMIT ?";

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
     * Saves a player's complete statistics to the database.
     * This will create a new row if one doesn't exist, or update the existing one.
     * @param stats The PlayerStats object to save.
     * @throws SQLException If a database error occurs.
     */
    public void savePlayerStats(PlayerStats stats) throws SQLException {
        String query;
        if (storageType == StorageType.MYSQL) {
            query = "INSERT INTO " + PLAYER_STATS_TABLE + " (uuid, total_distance, total_time_seconds, longest_flight, boosts_used, super_boosts_used, plugin_saves) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "total_distance = ?, total_time_seconds = ?, longest_flight = ?, boosts_used = ?, super_boosts_used = ?, plugin_saves = ?";
        } else {
            query = "REPLACE INTO " + PLAYER_STATS_TABLE + " (uuid, total_distance, total_time_seconds, longest_flight, boosts_used, super_boosts_used, plugin_saves) " +
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


    public final void save() throws SQLException {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats stats = this.plugin.getStatsHandler().getStats(player);
            if (stats != null)
                this.plugin.getDatabaseHandler().savePlayerStats(stats);
        }

        if (!this.plugin.getConfigHandlerInstance().getIsTimeLimitEnabled())
            return;

        this.plugin.getElytraFlightListener().validateFlightTimeOnReload();
    }


    public void setDatabaseVariables() {
        String typeFromConfig = plugin.getConfigHandlerInstance().getStorageType().toUpperCase();
        try {
            this.storageType = StorageType.valueOf(typeFromConfig);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid storage type '" + typeFromConfig + "' in config.yml. Defaulting to SQLITE.");
            this.storageType = StorageType.SQLITE;
        }

        // If using MySQL, load all the credentials
        if (this.storageType == StorageType.MYSQL) {
            this.host = plugin.getConfigHandlerInstance().getHost();
            this.port = plugin.getConfigHandlerInstance().getPort();
            this.database = plugin.getConfigHandlerInstance().getDatabase();
            this.username = plugin.getConfigHandlerInstance().getUsername();
            this.password = plugin.getConfigHandlerInstance().getPassword();
        }
    }


    private void InitializeTables() throws SQLException {
        if (storageType == StorageType.MYSQL) {
            String createTableQuery = """
                CREATE TABLE IF NOT EXISTS elytra_flight_time (
                    uuid VARCHAR(36) PRIMARY KEY,
                    flight_time INT DEFAULT 0
                );
                """;

            String createOwnedEffectsTableQuery = """
                CREATE TABLE IF NOT EXISTS owned_effects (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    effect_key VARCHAR(255) NOT NULL,
                    is_active BOOLEAN NOT NULL DEFAULT FALSE,
                    owned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

            String createPlayerStatsTableQuery = """
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid VARCHAR(36) PRIMARY KEY,
                    total_distance DOUBLE DEFAULT 0,
                    total_time_seconds BIGINT DEFAULT 0,
                    longest_flight DOUBLE DEFAULT 0,
                    boosts_used INT DEFAULT 0,
                    super_boosts_used INT DEFAULT 0,
                    plugin_saves INT DEFAULT 0
                );
                """;

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createTableQuery);
                stmt.executeUpdate(createOwnedEffectsTableQuery);
                stmt.executeUpdate(createPlayerStatsTableQuery);
            }

        } else {
            String createFlightTimeTableQuery = """
                CREATE TABLE IF NOT EXISTS elytra_flight_time (
                    uuid TEXT PRIMARY KEY,
                    flight_time INTEGER DEFAULT 0
                );
                """;

            String createOwnedEffectsTableQuery = """
                CREATE TABLE IF NOT EXISTS owned_effects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    effect_key TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 0,
                    owned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

            String createPlayerStatsTableQuery = """
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid TEXT PRIMARY KEY,
                    total_distance REAL DEFAULT 0,
                    total_time_seconds INTEGER DEFAULT 0,
                    longest_flight REAL DEFAULT 0,
                    boosts_used INTEGER DEFAULT 0,
                    super_boosts_used INTEGER DEFAULT 0,
                    plugin_saves INTEGER DEFAULT 0
                );
                """;

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createFlightTimeTableQuery);
                stmt.executeUpdate(createOwnedEffectsTableQuery);
                stmt.executeUpdate(createPlayerStatsTableQuery);
            }
        }
    }


    //<editor-fold desc="BACKUPS">

    public void startAutoBackupTask() {
        if (storageType != StorageType.SQLITE) {
            return;
        }

        if (this.backupTask != null && !this.backupTask.isCancelled()) {
            return;
        }

        //  1 hour
        long interval = AUTO_BACKUPS_TIME;
        this.backupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::backupSQLiteDatabase, interval, interval);
    }


    public void cancelBackupTask() {
        if (this.backupTask != null) {
            this.backupTask.cancel();
            this.backupTask = null;
        }
    }

    /**
     * Copies the current SQLite database file to a timestamped backup file.
     * This method is run asynchronously to prevent server lag.
     */
    private void backupSQLiteDatabase() {
        File databaseFolder = new File(plugin.getDataFolder(), "database");
        File sourceFile = new File(databaseFolder, SQLITE_DATABASE_NAME);

        if (!sourceFile.exists()) {
            plugin.getLogger().warning("SQLite database file not found. Skipping backup.");
            return;
        }

        File backupFolder = new File(databaseFolder, "backups");
        if (!backupFolder.exists()) {
            boolean wasCreated = backupFolder.mkdirs();
            if (!wasCreated) {
                plugin.getLogger().severe("Could not create backups folder! Please check file system permissions. Skipping backup.");
                return;
            }
        }

        File[] backupFiles = backupFolder.listFiles((dir, name) -> name.endsWith(".db"));
        if (backupFiles != null && backupFiles.length >= MAX_BACKUPS) {
            // Sort files by last modified date (oldest first)
            Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));

            // Delete the oldest backup file
            File oldestFile = backupFiles[0];
            if (oldestFile.delete()) {
                plugin.getMessagesHelper().sendDebugMessage("Deleted oldest backup file: " + oldestFile.getName());
            } else {
                plugin.getLogger().warning("Could not delete oldest backup file: " + oldestFile.getName());
            }
        }

        // --- Existing backup creation logic ---
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String formattedDate = formatter.format(new Date());

        String backupFileName = "backup_" + formattedDate + ".db";
        File destinationFile = new File(backupFolder, backupFileName);

        try {
            Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getMessagesHelper().sendDebugMessage("Successfully created database backup: " + destinationFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("FATAL: Could not create SQLite database backup! Check file permissions.");
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a list of all available SQLite backup file names.
     * @return A list of backup file names, or an empty list if none are found.
     */
    public List<String> getBackupFileNames() {
        // This check ensures the method only runs if SQLite is being used.
        if (storageType != StorageType.SQLITE) {
            return Collections.emptyList();
        }

        List<String> fileNames = new ArrayList<>();
        File backupFolder = new File(plugin.getDataFolder(), "database/backups");

        // Check if the backup folder exists and is a directory
        if (backupFolder.exists() && backupFolder.isDirectory()) {
            // List only files that end with .db
            File[] files = backupFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".db"));

            if (files != null) {
                for (File file : files) {
                    fileNames.add(file.getName());
                }
            }
        }
        return fileNames;
    }


    //</editor-fold>

}