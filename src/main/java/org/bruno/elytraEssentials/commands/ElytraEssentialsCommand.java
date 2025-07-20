package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

public class ElytraEssentialsCommand implements CommandExecutor, TabCompleter {
    private final Logger logger;
    private final MessagesHelper messagesHelper;

    private final Map<String, ISubCommand> subCommands = new HashMap<>();

    public ElytraEssentialsCommand(Logger logger, MessagesHelper messagesHelper) {
        this.logger = logger;
        this.messagesHelper = messagesHelper;
    }

    public void registerSubCommand(String name, ISubCommand command) {
        subCommands.put(name, command);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            try {
                subCommands.get("help").execute(sender, new String[0]);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "A database error occurred while executing command /ee help'" + "' for " + sender.getName(), e);
                messagesHelper.sendCommandSenderMessage(sender,"&cAn unexpected database error occurred. Please contact an administrator.");
                return true;
            }
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        ISubCommand commandHandler = subCommands.get(subCommandName);

        if (commandHandler == null) {
            messagesHelper.sendCommandSenderMessage(sender,"&cUnknown subcommand. Use /ee help for available commands.");
            return true;
        }

        try {
            // Pass only the arguments relevant to the subcommand
            String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
            return commandHandler.execute(sender, subCommandArgs);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "A database error occurred while executing command '" + subCommandName + "' for " + sender.getName(), e);
            messagesHelper.sendCommandSenderMessage(sender,"&cAn unexpected database error occurred. Please contact an administrator.");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred while executing command '" + subCommandName + "' for " + sender.getName(), e);
            messagesHelper.sendCommandSenderMessage(sender,"&cAn unexpected error occurred. Please contact an administrator.");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            // Player is typing the subcommand name
            List<String> completions = new ArrayList<>();

            // Use a helper method to check permissions for each subcommand
            if (PermissionsHelper.hasHelpPermission(sender)) completions.add("help");
            if (PermissionsHelper.hasReloadPermission(sender)) completions.add("reload");
            if (PermissionsHelper.hasFlightTimeCommandPermission(sender)) completions.add("ft");
            if (PermissionsHelper.hasShopPermission(sender)) completions.add("shop");
            if (PermissionsHelper.hasEffectsPermission(sender)) completions.add("effects");
            if (PermissionsHelper.hasStatsPermission(sender)) completions.add("stats");
            if (PermissionsHelper.hasTopPermission(sender)) completions.add("top");
            if (PermissionsHelper.hasForgePermission(sender)) completions.add("forge");
            if (PermissionsHelper.hasArmorPermission(sender)) completions.add("armor");
            if (PermissionsHelper.hasImportDbPermission(sender)) completions.add("importdb");
            if (PermissionsHelper.hasAchievementsPermission(sender)) completions.add("achievements");

            // Return suggestions that start with what the player has already typed
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length > 1) {
            // Player is typing arguments for a subcommand
            String subCommandName = args[0].toLowerCase();
            ISubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null) {
                // Pass only the relevant arguments to the subcommand's completer
                return subCommand.getSubcommandCompletions(sender, args);
            }
        }

        return List.of(); // Return an empty list if no suggestions are found
    }
}
