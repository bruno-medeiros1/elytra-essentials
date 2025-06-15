package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;

public class DatabaseHandler {
    private final static String ELYTRA_FLIGHT_TIME_TABLE = "elytra_flight_time";
    private final static String OWNED_EFFECTS_TABLE = "owned_effects";

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    private final ElytraEssentials elytraEssentials;

    private Connection connection;

    public DatabaseHandler(ElytraEssentials elytraEssentials){
        this.elytraEssentials = elytraEssentials;

        SetDatabaseVariables();

        //StartPeriodicSaving();
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
                this.elytraEssentials.getMessagesHelper().sendDebugMessage("Database was closed successfully!");
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

    public final void SetDatabaseVariables() {
        this.host = elytraEssentials.getConfigHandlerInstance().getHost();
        this.port = elytraEssentials.getConfigHandlerInstance().getPort();
        this.database = elytraEssentials.getConfigHandlerInstance().getDatabase();
        this.username = elytraEssentials.getConfigHandlerInstance().getUsername();
        this.password = elytraEssentials.getConfigHandlerInstance().getPassword();
    }

    private void InitializeTables() throws SQLException {
        String createTableQuery = """
                CREATE TABLE IF NOT EXISTS elytra_flight_time (
                    uuid VARCHAR(36) NOT NULL,
                    flight_time INT DEFAULT 0,
                    PRIMARY KEY (uuid)
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

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createTableQuery);
            stmt.executeUpdate(createOwnedEffectsTableQuery);
        }
    }

//    private void StartPeriodicSaving(){
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                for (Map.Entry<UUID, Integer> entry : elytraEssentials.getElytraFlightListener().GetAllActiveFlights().entrySet()) {
//                    try {
//                        SetPlayerFlightTime(entry.getKey(), entry.getValue());
//                    } catch (SQLException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//        }.runTaskTimerAsynchronously(elytraEssentials, 20 * 60 * 5, 20 * 60 * 5); // Every 5 minutes
//    }
}