package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.handlers.FlightHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlightTimeCommand implements ISubCommand {

    private final FlightHandler flightHandler;
    private final ConfigHandler configHandler;
    private final MessagesHelper messagesHelper;
    private final FoliaHelper foliaHelper;
    private final MessagesHandler messagesHandler;

    public FlightTimeCommand(FlightHandler flightHandler, ConfigHandler configHandler, MessagesHelper messagesHelper, FoliaHelper foliaHelper,
                             MessagesHandler messagesHandler) {
        this.flightHandler = flightHandler;
        this.configHandler = configHandler;
        this.messagesHelper = messagesHelper;
        this.foliaHelper = foliaHelper;
        this.messagesHandler = messagesHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!configHandler.getIsTimeLimitEnabled()) { /* ... */ return true; }
        if (!PermissionsHelper.hasFlightTimeCommandPermission(sender)) { /* ... */ return true; }
        if (args.length < 2) { /* send usage */ return true; }

        String action = args[0].toLowerCase();
        switch (action) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "set" -> handleSet(sender, args);
            case "clear" -> handleClear(sender, args);
            default -> sendUsageMessage(sender);
        }
        return true;
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Stream.of("add", "remove", "set", "clear")
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 4) {
            String action = args[1].toLowerCase();
            if (action.equals("add") || action.equals("remove") || action.equals("set")) {
                return List.of("60", "600", "3600");
            }
        }
        return List.of();
    }

    // Each 'handle' method is now simple: validate input, then delegate to the handler asynchronously.
    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) { sendUsageMessage(sender); return; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!validateTargetPlayer(sender, target, args[1])) return;
        int amount = parsePositiveInteger(args[2], sender);
        if (amount == -1) return;

        foliaHelper.runAsyncTask(() -> flightHandler.addFlightTime(target.getUniqueId(), amount, sender));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) { sendUsageMessage(sender); return; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!validateTargetPlayer(sender, target, args[1])) return;
        int amount = parsePositiveInteger(args[2], sender);
        if (amount == -1) return;

        foliaHelper.runAsyncTask(() -> flightHandler.removeFlightTime(target.getUniqueId(), amount, sender));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) { sendUsageMessage(sender); return; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!validateTargetPlayer(sender, target, args[1])) return;
        int amount = parsePositiveInteger(args[2], sender);
        if (amount == -1) return;

        foliaHelper.runAsyncTask(() -> flightHandler.setFlightTime(target.getUniqueId(), amount, sender));
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) { sendUsageMessage(sender); return; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!validateTargetPlayer(sender, target, args[1])) return;

        foliaHelper.runAsyncTask(() -> flightHandler.clearFlightTime(target.getUniqueId(), sender));
    }

    private boolean validateTargetPlayer(CommandSender sender, OfflinePlayer target, String targetName) {
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            String message = messagesHandler.getPlayerNotFound().replace("{0}", targetName);
            messagesHelper.sendCommandSenderMessage(sender, message);
            return false;
        }
        if (target.isOnline() && PermissionsHelper.playerBypassTimeLimit(target.getPlayer())) {
            messagesHelper.sendCommandSenderMessage(sender,"&cThe player " + targetName + " has time limit bypass and cannot be managed.");
            return false;
        }
        return true;
    }

    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage("§cUsage:");
        sender.sendMessage("§c/ee ft <add|remove|set> <player> <seconds>");
        sender.sendMessage("§c/ee ft clear <player>");
    }

    private int parsePositiveInteger(String input, CommandSender sender) {
        try {
            int value = Integer.parseInt(input);
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            messagesHelper.sendCommandSenderMessage(sender,"&cInvalid number. Please enter a positive integer.");
            return -1;
        }
    }
}