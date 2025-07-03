package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ImportDbCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public ImportDbCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("§cThis command can only be run by players or the console.");
            return true;
        }

        if (sender instanceof Player player) {
            if (!PermissionsHelper.hasImportDbPermission(player)) {
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
                return true;
            }
        }

        plugin.getLogger().info("Execute called: " + args.length);

        // Check if using SQLite
        if (!plugin.getConfigHandlerInstance().getStorageType().equalsIgnoreCase("SQLITE")) {
            sender.sendMessage(ChatColor.RED + "This command can only be used with the SQLite storage type.");
            return true;
        }

        // Argument Validation
        if (args.length < 1 ) {
            sender.sendMessage(ChatColor.RED + "Usage: /ee importdb <backup_filename>");
            sender.sendMessage(ChatColor.YELLOW + "Use tab-complete to see available backup files.");
            return true;
        }

        String backupFileName = args[0];
        File backupFile = new File(plugin.getDataFolder(), "database/backups/" + backupFileName);

        if (!backupFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Error: Backup file '" + backupFileName + "' not found.");
            return true;
        }

        // Confirmation Check
        if (args.length < 2 || !args[1].equalsIgnoreCase("--confirm")) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "§lWARNING: §r§cThis is a destructive operation!");
            sender.sendMessage(ChatColor.YELLOW + "This will overwrite all player's data with the selected backup.");
            sender.sendMessage(ChatColor.YELLOW + "All online players will be kicked from the server.");
            sender.sendMessage(ChatColor.GREEN + "To proceed, run the command again with '--confirm' at the end.");
            sender.sendMessage(ChatColor.GOLD + "§lThis action cannot be undone!");
            sender.sendMessage("");
            return true;
        }

        // --- Start the Import Process ---
        plugin.getMessagesHelper().sendConsoleMessage("&e###########################################");
        plugin.getMessagesHelper().sendConsoleMessage("&aStarting database import from backup: " + backupFileName);

        // 1. Kick all players
        plugin.getMessagesHelper().sendConsoleMessage("&aStarting import process... Kicking all players.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer("Server is restoring a data backup. Please reconnect in a moment.");
        }

        // 2. Shutdown plugin services
        plugin.getDatabaseHandler().Disconnect();
        plugin.cancelAllPluginTasks();

        // 3. Perform the file swap
        try {
            File databaseFolder = new File(plugin.getDataFolder(), "database");
            File liveDbFile = new File(databaseFolder, "elytraessentials.db");

            // Rename the current live DB as a final failsafe
            if (liveDbFile.exists()) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                File preImportBackup = new File(databaseFolder, "pre_import_backup_" + timestamp + ".db");
                Files.move(liveDbFile.toPath(), preImportBackup.toPath());
                plugin.getMessagesHelper().sendConsoleMessage("&aCreating a backup for the current live database: " + preImportBackup.getName());
                plugin.getMessagesHelper().sendConsoleMessage("&aThe above is a failsafe backup in case the import fails");
            }

            // Copy the backup file to become the new live database
            Files.copy(backupFile.toPath(), liveDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getMessagesHelper().sendConsoleMessage("&aSuccessfully restored backup file to live database.");
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "A critical file error occurred. Check the console for details.");
            e.printStackTrace();
            return true;
        }

        // 4. Re-initialize the plugin's services
        try {
            plugin.getDatabaseHandler().Initialize();
            plugin.startAllPluginTasks(); // Assumes you create this helper
            plugin.getMessagesHelper().sendConsoleMessage("&aDatabase import successful! Players can now reconnect.");
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Import failed during re-initialization. The server may need a restart. Check console.");
            e.printStackTrace();
        }

        plugin.getMessagesHelper().sendConsoleMessage("&e###########################################");
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
