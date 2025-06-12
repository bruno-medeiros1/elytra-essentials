package org.bruno.elytraEssentials.handlers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.UUID;

public class DatabaseHandler {
    private final static String ELYTRA_FLIGHT_TIME_TABLE = "elytra_flight_time";

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
                this.elytraEssentials.getMessagesHelper().SendDebugMessage("Database was closed successfully!");
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

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createTableQuery);
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