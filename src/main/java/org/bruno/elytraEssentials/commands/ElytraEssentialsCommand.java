package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

public class ElytraEssentialsCommand implements CommandExecutor, TabCompleter {

    private final ElytraEssentials plugin;
    private final Map<String, ISubCommand> subCommands = new HashMap<>();

    public ElytraEssentialsCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
        subCommands.put("help", new HelpCommand(plugin));
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("ft", new FlightTimeCommand(plugin));
        subCommands.put("shop", new ShopCommand(plugin));
        subCommands.put("effects", new EffectsCommand(plugin));
        subCommands.put("stats", new StatsCommand(plugin));
        subCommands.put("top", new TopCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /ee <subcommand>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        ISubCommand commandHandler = subCommands.get(subCommand);

        if (commandHandler == null) {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /ee help for available commands.");
            return true;
        }

        try {
            return commandHandler.Execute(sender, Arrays.copyOfRange(args, 1, args.length));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            // Player is typing the FIRST argument (the subcommand name)
            // e.g., /ee <HERE>

            // We'll return a list of all subcommand names they have permission to use.
            List<String> completions = new ArrayList<>();
            for (String subCommandName : subCommands.keySet()) {
                // TODO: Add a permission check here!
                // For example: if (sender.hasPermission("elytraessentials.command." + subCommandName)) { ... }
                completions.add(subCommandName);
            }
            // Return suggestions that start with what the player has already typed
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length > 1) {
            // Player is typing the SECOND (or third, etc.) argument
            // e.g., /ee top <HERE>
            
            String subCommandName = args[0].toLowerCase();
            ISubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null) {
                // We found the subcommand, now we ask it for the suggestions.
                return subCommand.getSubcommandCompletions(sender, args);
            }
        }

        // Return an empty list if no suggestions are found
        return List.of();
    }
}
