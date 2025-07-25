package org.bruno.elytraEssentials.commands;

import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.List;

public interface SubCommand {
    boolean execute(CommandSender sender, String[] args) throws SQLException;

    default List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        return List.of();
    }
}
