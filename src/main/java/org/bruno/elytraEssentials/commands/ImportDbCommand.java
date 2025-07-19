package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.Bukkit;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ImportDbCommand implements ISubCommand {

    private final ElytraEssentials plugin;
    private final Logger logger;
    private final MessagesHelper messagesHelper;
    private final DatabaseHandler databaseHandler;
    private final MessagesHandler messagesHandler;

    public ImportDbCommand(ElytraEssentials plugin, Logger logger, MessagesHelper messagesHelper, DatabaseHandler databaseHandler, MessagesHandler messagesHandler) {
        this.plugin = plugin;
        this.logger = logger;
        this.messagesHelper = messagesHelper;
        this.databaseHandler = databaseHandler;
        this.messagesHandler = messagesHandler;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasImportDbPermission(sender)) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getNoPermissionMessage());
            return true;
        }

        if (!databaseHandler.getStorageType().equalsIgnoreCase("SQLITE")) {
            messagesHelper.sendCommandSenderMessage(sender,"&cThis command can only be used with the SQLite storage type.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /ee importdb <backup_filename>");
            sender.sendMessage("§eUse tab-complete to see available backup files.");
            sender.sendMessage("§7If none are shown, please wait for your first backup file to be generated.");
            return true;
        }

        String backupFileName = args[0];
        File backupFile = new File(plugin.getDataFolder(), Constants.Files.DB_FOLDER + "/" + Constants.Files.DB_BACKUP_FOLDER + "/" + backupFileName);

        if (!backupFile.exists()) {
            messagesHelper.sendCommandSenderMessage(sender,"&cError: Backup file '" + backupFileName + "' not found.");
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("--confirm")) {
            sender.sendMessage("");
            sender.sendMessage("§c§lWARNING: §r§cThis is a destructive operation!");
            sender.sendMessage("§eThis will overwrite all current player data with the selected backup.");
            sender.sendMessage("§eAll online players will be kicked from the server.");
            sender.sendMessage("§6To proceed, run the command again with '--confirm' at the end.");
            sender.sendMessage("§f/ee importdb " + backupFileName + " --confirm");
            sender.sendMessage("");
            return true;
        }

        // Start the Import Process
        logger.warning("Starting database import from backup: " + backupFileName);
        messagesHelper.sendCommandSenderMessage(sender,"&eStarting import process... Kicking all players.");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer("Server is restoring a data backup. Please reconnect in a moment.");
        }

        logger.info("All players kicked. Shutting down plugin services...");
        databaseHandler.Disconnect();
        plugin.shutdown();

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
                logger.info("Backup file verified successfully.");
            } catch (SQLException e) {
                messagesHelper.sendCommandSenderMessage(sender,"&cImport failed! The selected backup file appears to be corrupted. No changes were made.");
                logger.log(Level.SEVERE, "Could not verify the integrity of the backup file '" + backupFileName + "'. Aborting import.", e);

                if (!tempDbFile.delete()) {
                    logger.warning("Could not delete temporary database file: " + tempDbFile.getName());
                }

                databaseHandler.Initialize(); // Reconnect to the original database
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
            logger.info("Successfully restored backup file to live database.");

        } catch (IOException | SQLException e) {
            messagesHelper.sendCommandSenderMessage(sender,"&cA critical file error occurred. Check the console for details.");
            logger.log(Level.SEVERE, "Failed to perform database file swap during import.", e);
            return true;
        }

        try {
            logger.info("Re-initializing plugin services...");
            databaseHandler.Initialize();
            plugin.startAllPluginTasks();

            messagesHelper.sendCommandSenderMessage(sender,"&aDatabase import successful! Players can now reconnect.");
            logger.info("Database import complete.");
        } catch (SQLException e) {
            messagesHelper.sendCommandSenderMessage(sender,"&cImport failed during re-initialization. The server may need a restart. Check console.");
            logger.log(Level.SEVERE, "Failed to re-initialize services after database import.", e);
        }

        return true;
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Suggest backup file names
            List<String> backupFiles = databaseHandler.getBackupFileNames();
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