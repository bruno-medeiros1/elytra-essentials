package org.bruno.elytraEssentials.interfaces;

import org.bukkit.command.CommandSender;

import java.sql.SQLException;

public interface ISubCommand {
    boolean Execute(CommandSender sender, String[] args) throws SQLException;
}
