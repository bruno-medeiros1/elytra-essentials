package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ElytraEssentialsCommand implements CommandExecutor {
    private final Map<String, ISubCommand> subCommands = new HashMap<>();

    public ElytraEssentialsCommand(ElytraEssentials plugin) {
        subCommands.put("help", new HelpCommand());
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("flighttime", new FlightTimeCommand(plugin));
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

        return commandHandler.Execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
