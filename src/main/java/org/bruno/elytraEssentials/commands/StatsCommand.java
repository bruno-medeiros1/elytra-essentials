package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.helpers.*;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class StatsCommand implements ISubCommand {
    private final StatsHandler statsHandler;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler;

    public StatsCommand(StatsHandler statsHandler, MessagesHelper messagesHelper, MessagesHandler messagesHandler) {
        this.statsHandler = statsHandler;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflinePlayer target;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messagesHelper.sendCommandSenderMessage(sender,"&cUsage: /ee stats <player>");
                return true;
            }
            if (!PermissionsHelper.hasStatsPermission(player)) {
                messagesHelper.sendPlayerMessage(player, messagesHandler.getNoPermissionMessage());
                return true;
            }
            target = player;
        } else {
            if (!PermissionsHelper.hasStatsOthersPermission(sender)) {
                messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getNoPermissionMessage());
                return true;
            }
            target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getPlayerNotFound().replace("{0}",  args[0]));
                return true;
            }
        }

        // Delegate the entire process to the handler.
        statsHandler.fetchAndDisplayStats(sender, target);
        return true;
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (PermissionsHelper.hasStatsOthersPermission(sender)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }
}