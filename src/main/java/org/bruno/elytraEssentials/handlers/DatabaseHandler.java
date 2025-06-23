package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;

public class DatabaseHandler {
    private final static String ELYTRA_FLIGHT_TIME_TABLE = "elytra_flight_time";
    private final static String OWNED_EFFECTS_TABLE = "owned_effects";
    private final static String PLAYER_STATS_TABLE = "player_stats";

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    private final ElytraEssentials plugin;

    private Connection connection;

    public DatabaseHandler(ElytraEssentials plugin){
        this.plugin = plugin;

        SetDatabaseVariables();

        //  TODO: Add StartPeriodicSaving() to save data hourly
    }

    public void Initialize() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false",
                this.username,
                this.password
        );

        // Create the table if it doesn't exist
        InitializeTables();
    }

    public boolean IsConnected() {
        return connection != null;
    }

    public void Disconnect(){
        if (IsConnected()) {
            try {
                connection.close();
                this.plugin.getMessagesHelper().sendDebugMessage("Database was closed successfully!");
            } catch (SQLException e){
                Bukkit.getLogger().severe("Failed to close the connection to the database.");
                Bukkit.getLogger().severe("Error: " + e.getMessage());
                Bukkit.getLogger().severe("Stack Trace:");
                for (StackTraceElement element : e.getStackTrace()) {
                    Bukkit.getLogger().severe("  at " + element.toString());
                }
            }
        }
    }

    public int GetPlayerFlightTime(UUID uuid) throws SQLException {
        String query = "SELECT flight_time FROM " + ELYTRA_FLIGHT_TIME_TABLE + " WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("flight_time");
                }
            }
        }
        return 0; // Default to 0 if not found
    }

    public void SetPlayerFlightTime(UUID uuid, int time) throws SQLException {
        String query = "INSERT INTO " + ELYTRA_FLIGHT_TIME_TABLE + " (uuid, flight_time) VALUES (?, ?) ON DUPLICATE KEY UPDATE flight_time = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, time);
            stmt.setInt(3, time);
            stmt.executeUpdate();
        }
    }

    public void AddOwnedEffect(UUID playerUuid, String effectKey) throws SQLException {
        String query = "INSERT INTO " + OWNED_EFFECTS_TABLE + " (player_uuid, effect_key) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, effectKey);
            stmt.executeUpdate();
        }
    }

    public void UpdateOwnedEffect(UUID playerId, String effectKey, boolean isActive) throws SQLException {
        String query = "UPDATE " + OWNED_EFFECTS_TABLE + " SET is_active = ? WHERE player_uuid = ? AND effect_key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setBoolean(1, isActive); // Use the passed boolean parameter
            stmt.setString(2, playerId.toString());
            stmt.setString(3, effectKey);
            stmt.executeUpdate();
        }
    }

    public boolean GetIsActiveOwnedEffect(UUID playerId, String effectKey) throws SQLException {
        String query = "SELECT is_active FROM " + OWNED_EFFECTS_TABLE + " WHERE player_uuid = ? AND effect_key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, effectKey);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_active"); // Fetch the is_active value
                } else {
                    throw new SQLException("No matching record found for player_uuid and effect_id.");
                }
            }
        }
    }

    public List<String> GetOwnedEffectKeys(UUID playerId) throws SQLException {
        List<String> ownedEffects = new ArrayList<>();
        String query = "SELECT effect_key FROM " + OWNED_EFFECTS_TABLE + " WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ownedEffects.add(rs.getString("effect_key"));
                }
            }
        }
        return ownedEffects;
    }

    public String getPlayerActiveEffect(UUID playerId) throws SQLException {
        String query = "SELECT effect_key FROM " + OWNED_EFFECTS_TABLE + " WHERE player_uuid = ? AND is_active = TRUE";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("effect_key");
                }
            }
        }

        // Return null if no active effect is found
        return null;
    }

    /**
     * Retrieves all statistics for a given player.
     * If the player has no stats entry, a new default stats object is returned.
     *
     * @param uuid The UUID of the player.
     * @return A PlayerStats object containing all their stats.
     * @throws SQLException If a database error occurs.
     */
    public PlayerStats getPlayerStats(UUID uuid) throws SQLException {
        String query = "SELECT * FROM " + PLAYER_STATS_TABLE + " WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Player found, create a stats object from the data
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
        // No entry found for this player, return a new object with default (zero) values
        return new PlayerStats(uuid);
    }

    /**
     * Resets all statistics for a given player back to their default zero values.
     * @param uuid The UUID of the player to reset.
     * @throws SQLException If a database error occurs.
     */
    public void resetPlayerStats(UUID uuid) throws SQLException {
        String query = "UPDATE " + PLAYER_STATS_TABLE + " SET " +
                "total_distance = 0, " +
                "total_time_seconds = 0, " +
                "longest_flight = 0, " +
                "boosts_used = 0, " +
                "super_boosts_used = 0, " +
                "plugin_saves = 0 " +
                "WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        }
    }

    /**
     * Saves a player's complete statistics to the database.
     * This will create a new row if one doesn't exist, or update the existing one.
     * @param stats The PlayerStats object to save.
     * @throws SQLException If a database error occurs.
     */
    public void savePlayerStats(PlayerStats stats) throws SQLException {
        String query = "INSERT INTO " + PLAYER_STATS_TABLE + " (uuid, total_distance, total_time_seconds, longest_flight, boosts_used, super_boosts_used, plugin_saves) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                "total_distance = ?, total_time_seconds = ?, longest_flight = ?, boosts_used = ?, super_boosts_used = ?, plugin_saves = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            // Values for INSERT
            stmt.setString(1, stats.getUuid().toString());
            stmt.setDouble(2, stats.getTotalDistance());
            stmt.setLong(3, stats.getTotalTimeSeconds());
            stmt.setDouble(4, stats.getLongestFlight());
            stmt.setInt(5, stats.getBoostsUsed());
            stmt.setInt(6, stats.getSuperBoostsUsed());
            stmt.setInt(7, stats.getPluginSaves());

            // Values for UPDATE
            stmt.setDouble(8, stats.getTotalDistance());
            stmt.setLong(9, stats.getTotalTimeSeconds());
            stmt.setDouble(10, stats.getLongestFlight());
            stmt.setInt(11, stats.getBoostsUsed());
            stmt.setInt(12, stats.getSuperBoostsUsed());
            stmt.setInt(13, stats.getPluginSaves());

            stmt.executeUpdate();
        }
    }

    public final void SetDatabaseVariables() {
        this.host = plugin.getConfigHandlerInstance().getHost();
        this.port = plugin.getConfigHandlerInstance().getPort();
        this.database = plugin.getConfigHandlerInstance().getDatabase();
        this.username = plugin.getConfigHandlerInstance().getUsername();
        this.password = plugin.getConfigHandlerInstance().getPassword();
    }

    public final void save() throws SQLException {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats stats = this.plugin.getStatsHandler().getStats(player);
            if (stats != null)
                this.plugin.getDatabaseHandler().savePlayerStats(stats);

            //  Time Flight if feature is enabled
            if (!this.plugin.getConfigHandlerInstance().getIsTimeLimitEnabled())
                return;
        }

        this.plugin.getElytraFlightListener().validateFlightTimeOnReload();
    }

    private void InitializeTables() throws SQLException {
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
    }
}