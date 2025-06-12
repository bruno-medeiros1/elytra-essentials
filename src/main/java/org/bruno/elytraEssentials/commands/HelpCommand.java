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
            sender.sendMessage(ChatColor.GRAY + "/ee flighttime add <player> <seconds> - Add flight time.");
            return true;
        }

        return true;
    }
}
