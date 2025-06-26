package org.bruno.elytraEssentials.interfaces;

import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.List;

public interface ISubCommand {
    boolean Execute(CommandSender sender, String[] args) throws SQLException;

    default List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        return List.of();
    }
}
