package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class FlightTimeCommand implements ISubCommand {

    private final ElytraEssentials plugin;
    private final DatabaseHandler databaseHandler;

    private int maxTimeLimit = 0;

    public FlightTimeCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.databaseHandler = plugin.getDatabaseHandler();
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("§cThis command can only be run by players or the console.");
            return true;
        }

        boolean isTimeLimitEnabled = plugin.getConfigHandlerInstance().getIsTimeLimitEnabled();
        if (!isTimeLimitEnabled) {
            sender.sendMessage("§cThis feature is not enabled.");
            return true;
        }

        MessagesHandler messagesHandler = this.plugin.getMessagesHandlerInstance();
        MessagesHelper messagesHelper = this.plugin.getMessagesHelper();

        // Permission check for players
        if (sender instanceof Player && !sender.hasPermission("elytraEssentials.*")
                && !sender.hasPermission("elytraessentials.command.*")
                && !sender.hasPermission("elytraEssentials.command.flighttime")) {
            messagesHelper.sendPlayerMessage((Player) sender, messagesHandler.getNoPermissionMessage());
            return true;
        }

        // Validate arguments
        if (args.length < 2) {
            sendUsageMessage(sender);
            return true;
        }

        // Get the target player
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("&cPlayer " + args[1] + " not found.");
            return true;
        }

        //  Check if target player has time limit bypass permission
        if (target.hasPermission("elytraessentials.*") || target.hasPermission("elytraessentials.bypass.*")
            || target.hasPermission("elytraessentials.bypass.timelimit")){
            sender.sendMessage("§cThe player " + args[1] + " has time limit bypass");
            return true;
        }

        // Handle the command action
        String action = args[0].toLowerCase();
        try {
            maxTimeLimit = plugin.getConfigHandlerInstance().getMaxTimeLimit();

            if (!processCommandAction(sender, action, args, target)) {
                sendUsageMessage(sender);
            }
        } catch (Exception e) {
            sender.sendMessage("§cAn error occurred while processing the command.");
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Processes the command action and returns whether it was successfully handled.
     */
    private boolean processCommandAction(CommandSender sender, String action, String[] args, Player target) {
        int amount;
        switch (action) {
            case "add":
            case "remove":
            case "set":
                if (args.length < 3) return false;

                amount = parsePositiveInteger(args[2], sender);
                if (amount == -1) return true;

                switch (action) {
                    case "add":
                        addFlightTime(sender, target, amount);
                        break;
                    case "remove":
                        removeFlightTime(sender, target, amount);
                        break;
                    case "set":
                        setFlightTime(sender, target, amount);
                        break;
                }
                break;

            case "clear":
                clearFlightTime(sender, target);
                break;

            default:
                return false;
        }
        return true;
    }

    private void addFlightTime(CommandSender sender, Player player, int secondsToAdd) {
        try {
            int currentFlightTime = databaseHandler.GetPlayerFlightTime(player.getUniqueId());
            int newFlightTime = currentFlightTime + secondsToAdd;

            //  check if the user defined a max flight time
            if (maxTimeLimit > 0 && newFlightTime > maxTimeLimit){
                secondsToAdd = maxTimeLimit - currentFlightTime;
                newFlightTime = maxTimeLimit;
            }

            databaseHandler.SetPlayerFlightTime(player.getUniqueId(), newFlightTime);
            plugin.getElytraFlightListener().UpdatePlayerFlightTime(player.getUniqueId(), newFlightTime);

            sender.sendMessage("§aAdded " + secondsToAdd + " seconds of flight time to " + player.getName() + ".");

            String message = this.plugin.getMessagesHandlerInstance().getElytraFlightTimeAdded().replace("{0}", String.valueOf(secondsToAdd));
            plugin.getMessagesHelper().sendPlayerMessage(player, message);
        }
        catch (Exception e) {
            sender.sendMessage("§cFailed to add flight time.");
            e.printStackTrace();
        }
    }

    private void removeFlightTime(CommandSender sender, Player player, int secondsToRemove) {
        try {
            int currentFlightTime = databaseHandler.GetPlayerFlightTime(player.getUniqueId());
            int newFlightTime = Math.max(0, currentFlightTime - secondsToRemove);

            databaseHandler.SetPlayerFlightTime(player.getUniqueId(), newFlightTime);
            plugin.getElytraFlightListener().UpdatePlayerFlightTime(player.getUniqueId(), newFlightTime);

            if (newFlightTime == 0) {
                sender.sendMessage("§aRemoved all flight time from " + player.getName() + ".");
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getElytraFlightTimeCleared());
            }
            else {
                sender.sendMessage("§aRemoved " + secondsToRemove + " seconds of flight time from " + player.getName() + ".");
                String message = this.plugin.getMessagesHandlerInstance().getElytraFlightTimeRemoved().replace("{0}", String.valueOf(secondsToRemove));
                plugin.getMessagesHelper().sendPlayerMessage(player, message);
            }
        } catch (Exception e) {
            sender.sendMessage("§cFailed to remove flight time.");
            e.printStackTrace();
        }
    }

    private void setFlightTime(CommandSender sender, Player player, int newFlightTime) {
        try {
            //  check if the user defined a max flight time
            if (maxTimeLimit > 0 &&  newFlightTime > maxTimeLimit) {
                newFlightTime = maxTimeLimit;
                sender.sendMessage("§aSet " + player.getName() + "'s flight time to max time of: " + maxTimeLimit + " seconds.");
            }
            else{
                sender.sendMessage("§aSet " + player.getName() + "'s flight time to " + newFlightTime + " seconds.");
            }

            databaseHandler.SetPlayerFlightTime(player.getUniqueId(), newFlightTime);
            plugin.getElytraFlightListener().UpdatePlayerFlightTime(player.getUniqueId(), newFlightTime);

            String message = this.plugin.getMessagesHandlerInstance().getElytraFlightTimeSet().replace("{0}", String.valueOf(newFlightTime));
            plugin.getMessagesHelper().sendPlayerMessage(player, message);
        }
        catch (Exception e) {
            sender.sendMessage("§cFailed to set flight time.");
            e.printStackTrace();
        }
    }

    private void clearFlightTime(CommandSender sender, Player player) {
        try {
            databaseHandler.SetPlayerFlightTime(player.getUniqueId(), 0);
            plugin.getElytraFlightListener().UpdatePlayerFlightTime(player.getUniqueId(), 0);

            sender.sendMessage("§aCleared all flight time for " + player.getName() + ".");
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getElytraFlightTimeCleared());
        }
        catch (Exception e) {
            sender.sendMessage("§cFailed to clear flight time.");
            e.printStackTrace();
        }
    }

    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage("§cUsage:");
        sender.sendMessage("§c/ee ft add <player> <seconds>");
        sender.sendMessage("§c/ee ft remove <player> <seconds>");
        sender.sendMessage("§c/ee ft set <player> <seconds>");
        sender.sendMessage("§c/ee ft clear <player>");
    }

    private int parsePositiveInteger(String input, CommandSender sender) {
        try {
            int value = Integer.parseInt(input);
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number. Please enter a positive integer.");
            return -1;
        }
    }
}