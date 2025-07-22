package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.helpers.*;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
        // Case 1: /ee stats (args.length is 0)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messagesHelper.sendCommandSenderMessage(sender, "&cUsage: /ee stats <player> OR /ee stats <subcommand>");
                return true;
            }
            if (!PermissionsHelper.hasStatsPermission(player)) {
                messagesHelper.sendPlayerMessage(player, messagesHandler.getNoPermissionMessage());
                return true;
            }
            statsHandler.fetchAndDisplayStats(sender, player);
            return true;
        }

        // Case 2: Arguments are present. Check args[0] for a subcommand.
        String subcommand = args[0].toLowerCase();
        if ("reset".equals(subcommand)) {
            handleReset(sender, args);
        } else {
            // If not a subcommand, assume args[0] is a player name.
            handleView(sender, args);
        }

        return true;
    }

    private void handleView(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasStatsOthersPermission(sender)) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getNoPermissionMessage());
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getPlayerNotFound().replace("{0}", args[0]));
            return;
        }
        statsHandler.fetchAndDisplayStats(sender, target);
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasStatsResetPermission(sender)) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getNoPermissionMessage());
            return;
        }
        // The player name is now at index 1.
        if (args.length < 2) {
            messagesHelper.sendCommandSenderMessage(sender, "&cUsage: /ee stats reset <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getPlayerNotFound().replace("{0}", args[1]));
            return;
        }

        // The confirmation flag is now at index 2.
        if (args.length < 3 || !args[2].equalsIgnoreCase("--confirm")) {
            messagesHelper.sendCommandSenderMessage(sender, "&c&lWARNING: &r&cThis will permanently delete all stats for " + target.getName() + ".");
            messagesHelper.sendCommandSenderMessage(sender, "&eTo proceed, add '--confirm' to the end of the command.");
            return;
        }

        statsHandler.resetPlayerStats(target, sender);
    }
    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        // Tab completion for the argument after "stats": /ee stats <arg>
        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            String input = args[1].toLowerCase();

            if (PermissionsHelper.hasStatsResetPermission(sender)) {
                completions.add("reset");
            }
            if (PermissionsHelper.hasStatsOthersPermission(sender)) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        // Tab completion for the argument after "reset": /ee stats reset <player>
        if (args.length == 3 && args[1].equalsIgnoreCase("reset")) {
            if (PermissionsHelper.hasStatsResetPermission(sender)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        // Tab completion for the confirmation flag: /ee stats reset <player> <--confirm>
        if (args.length == 4 && args[1].equalsIgnoreCase("reset")) {
            if ("--confirm".startsWith(args[3].toLowerCase())) {
                return List.of("--confirm");
            }
        }

        return List.of();
    }
}