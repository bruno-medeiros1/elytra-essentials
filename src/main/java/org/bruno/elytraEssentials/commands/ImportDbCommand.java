package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ImportDbCommand implements SubCommand {
    private final ElytraEssentials plugin;
    private final MessagesHandler messagesHandler;
    private final MessagesHelper messagesHelper;
    private final DatabaseHandler databaseHandler;

    public ImportDbCommand(ElytraEssentials plugin, MessagesHandler messagesHandler, MessagesHelper messagesHelper, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.messagesHandler = messagesHandler;
        this.messagesHelper = messagesHelper;
        this.databaseHandler = databaseHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
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

        // Delegate the entire complex process to the handler
        databaseHandler.importFromBackup(backupFileName, sender, backupFile);
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