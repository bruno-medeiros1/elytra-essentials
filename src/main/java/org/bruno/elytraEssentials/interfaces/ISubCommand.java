package org.bruno.elytraEssentials.interfaces;

import org.bukkit.command.CommandSender;

public interface ISubCommand {
    boolean Execute(CommandSender sender, String[] args);
}
