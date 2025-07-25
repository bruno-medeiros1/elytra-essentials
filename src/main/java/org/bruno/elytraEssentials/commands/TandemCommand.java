package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.handlers.TandemHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TandemCommand implements SubCommand {

    private final TandemHandler tandemHandler;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler;
    private final ConfigHandler configHandler;

    public TandemCommand(TandemHandler tandemHandler, MessagesHelper messagesHelper, MessagesHandler messagesHandler, ConfigHandler configHandler) {
        this.tandemHandler = tandemHandler;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;
        this.configHandler = configHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messagesHelper.sendCommandSenderMessage(sender, "&cThis command can only be run by a player.");
            return true;
        }

        if (!configHandler.getIsTandemFlightEnabled()) {
            messagesHelper.sendPlayerMessage(player, messagesHandler.getFeatureNotEnabled());
            return true;
        }

        if (args.length == 0) {
            sendUsageMessage(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "leave" -> handleLeave(player);
            default -> sendUsageMessage(player);
        }
        return true;
    }

    private void handleInvite(Player driver, String[] args) {
        if (!PermissionsHelper.hasTandemFlightPermission(driver)) {
            messagesHelper.sendPlayerMessage(driver, messagesHandler.getNoPermissionMessage());
            return;
        }

        if (args.length < 2) {
            messagesHelper.sendPlayerMessage(driver, "&cUsage: /ee tandem invite <player>");
            return;
        }

        Player invitee = Bukkit.getPlayer(args[1]);
        if (invitee == null) {
            messagesHelper.sendPlayerMessage(driver, messagesHandler.getPlayerNotFound().replace("{0}", args[1]));
            return;
        }

        if (driver.equals(invitee)) {
            messagesHelper.sendPlayerMessage(driver, "&cYou cannot invite yourself to a tandem flight.");
            return;
        }

        tandemHandler.invitePlayer(driver, invitee);
    }

    private void handleAccept(Player passenger) {
        if (!PermissionsHelper.hasTandemFlightPermission(passenger)) {
            messagesHelper.sendPlayerMessage(passenger, messagesHandler.getNoPermissionMessage());
            return;
        }

        tandemHandler.acceptInvite(passenger);
    }

    private void handleLeave(Player passenger) {
        tandemHandler.dismountPassenger(passenger, null, true);
    }

    private void sendUsageMessage(CommandSender sender) {
        messagesHelper.sendCommandSenderMessage(sender, "&6&lTandem Flight Commands");
        messagesHelper.sendCommandSenderMessage(sender, "&e/ee tandem invite <player> &7- Invite a player to fly with you.");
        messagesHelper.sendCommandSenderMessage(sender, "&e/ee tandem accept &7- Accept a pending flight invitation.");
        messagesHelper.sendCommandSenderMessage(sender, "&e/ee tandem leave &7- Dismount from a tandem flight.");
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        // Tab completion for the first argument: /ee tandem <subcommand>
        if (args.length == 2) {
            List<String> subcommands = new ArrayList<>();
            if (PermissionsHelper.hasTandemInvitePermission(player)) subcommands.add("invite");
            if (PermissionsHelper.hasTandemAcceptPermission(player)) subcommands.add("accept");
            if (PermissionsHelper.hasTandemLeavePermission(player)) subcommands.add("leave");

            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Tab completion for the second argument: /ee tandem invite <player>
        if (args.length == 3 && args[1].equalsIgnoreCase("invite")) {
            if (PermissionsHelper.hasTandemInvitePermission(player)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> !name.equals(player.getName())) // Don't suggest self
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }
}
