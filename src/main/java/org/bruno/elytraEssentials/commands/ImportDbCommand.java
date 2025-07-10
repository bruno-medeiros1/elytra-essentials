package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ImportDbCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public ImportDbCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasImportDbPermission(sender)) {
            sender.sendMessage(ColorHelper.parse(plugin.getMessagesHandlerInstance().getNoPermissionMessage()));
            return true;
        }

        if (!plugin.getDatabaseHandler().getStorageType().equalsIgnoreCase("SQLITE")) {
            sender.sendMessage(ChatColor.RED + "This command can only be used with the SQLite storage type.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /ee importdb <backup_filename>");
            sender.sendMessage(ChatColor.YELLOW + "Use tab-complete to see available backup files.");
            return true;
        }

        String backupFileName = args[0];
        File backupFile = new File(plugin.getDataFolder(), Constants.Files.DB_FOLDER + "/" + Constants.Files.DB_BACKUP_FOLDER + "/" + backupFileName);

        if (!backupFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Error: Backup file '" + backupFileName + "' not found.");
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("--confirm")) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "§lWARNING: §r§cThis is a destructive operation!");
            sender.sendMessage(ChatColor.YELLOW + "This will overwrite all current player data with the selected backup.");
            sender.sendMessage(ChatColor.YELLOW + "All online players will be kicked from the server.");
            sender.sendMessage(ChatColor.GOLD + "To proceed, run the command again with '--confirm' at the end.");
            sender.sendMessage(ChatColor.WHITE + "/ee importdb " + backupFileName + " --confirm");
            sender.sendMessage("");
            return true;
        }

        // Start the Import Process
        plugin.getMessagesHelper().sendConsoleLog("warning", "Starting database import from backup: " + backupFileName);
        sender.sendMessage(ChatColor.YELLOW + "Starting import process... Kicking all players.");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer("Server is restoring a data backup. Please reconnect in a moment.");
        }

        plugin.getMessagesHelper().sendConsoleLog("info", "All players kicked. Shutting down plugin services...");
        plugin.getDatabaseHandler().Disconnect();
        plugin.cancelAllPluginTasks();

        File databaseFolder = new File(plugin.getDataFolder(), Constants.Files.DB_FOLDER);
        File liveDbFile = new File(databaseFolder, Constants.Files.SQLITE_DB_NAME);
        File tempDbFile = new File(databaseFolder, "import_temp.db");

        try {
            // 1. STAGING: Copy the backup to a temporary file.
            Files.copy(backupFile.toPath(), tempDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 2. VERIFICATION: Try to connect to the temporary database to ensure it's valid.
            try (Connection testConnection = DriverManager.getConnection("jdbc:sqlite:" + tempDbFile.getAbsolutePath())) {
                if (!testConnection.isValid(2)) {
                    throw new SQLException("Import verification failed: Backup file is corrupted or invalid.");
                }
                plugin.getMessagesHelper().sendConsoleLog("info", "Backup file verified successfully.");
            } catch (SQLException e) {
                sender.sendMessage(ChatColor.RED + "Import failed! The selected backup file appears to be corrupted. No changes were made.");
                plugin.getLogger().log(Level.SEVERE, "Could not verify the integrity of the backup file '" + backupFileName + "'. Aborting import.", e);

                if (!tempDbFile.delete()) {
                    plugin.getLogger().warning("Could not delete temporary database file: " + tempDbFile.getName());
                }

                plugin.getDatabaseHandler().Initialize(); // Reconnect to the original database
                plugin.startAllPluginTasks();
                return true;
            }

            // 3. COMMIT: If verification passed, perform the final swap.
            if (liveDbFile.exists()) {
                if (!liveDbFile.delete()) {
                    throw new IOException("Could not delete the old live database file. Aborting...");
                }
            }
            if (!tempDbFile.renameTo(liveDbFile)) {
                throw new IOException("Could not rename temporary database file to the live database file. Aborting...");
            }
            plugin.getMessagesHelper().sendConsoleLog("info", "Successfully restored backup file to live database.");

        } catch (IOException | SQLException e) {
            sender.sendMessage(ChatColor.RED + "A critical file error occurred. Check the console for details.");
            plugin.getLogger().log(Level.SEVERE, "Failed to perform database file swap during import.", e);
            return true;
        }

        try {
            plugin.getMessagesHelper().sendConsoleLog("info", "Re-initializing plugin services...");
            plugin.getDatabaseHandler().Initialize();
            plugin.startAllPluginTasks();
            sender.sendMessage(ChatColor.GREEN + "Database import successful! Players can now reconnect.");
            plugin.getMessagesHelper().sendConsoleLog("info", "Database import complete.");
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Import failed during re-initialization. The server may need a restart. Check console.");
            plugin.getLogger().log(Level.SEVERE, "Failed to re-initialize services after database import.", e);
        }

        return true;
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Suggest backup file names
            List<String> backupFiles = plugin.getDatabaseHandler().getBackupFileNames();
            return backupFiles.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            // Suggest the confirmation flag
            if ("--confirm".startsWith(args[2].toLowerCase())) {
                return List.of("--confirm");
            }
        }
        return List.of();
    }
}
