package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class HelpCommand implements ISubCommand {

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (sender instanceof Player || sender instanceof ConsoleCommandSender) {
            sender.sendMessage(ChatColor.YELLOW + "ElytraEssentials Commands:");
            sender.sendMessage(ChatColor.GRAY + "/ee reload - Reload the plugin.");
            sender.sendMessage(ChatColor.GRAY + "/ee shop - Opens the effects shop GUI.");
            sender.sendMessage(ChatColor.GRAY + "/ee effects  - Opens the owned effects GUI.");
            sender.sendMessage(ChatColor.GRAY + "/ee ft <subcommand> - Flight time related commands.");
            return true;
        }

        return true;
    }
}
