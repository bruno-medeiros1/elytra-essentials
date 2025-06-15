package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlightTimeCommand implements ISubCommand {

    private final ElytraEssentials plugin;
    private final DatabaseHandler databaseHandler;

    public FlightTimeCommand(ElytraEssentials elytraEssentials) {
        this.plugin = elytraEssentials;
        this.databaseHandler = this.plugin.getDatabaseHandler();
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsageMessage(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        try {
            switch (action) {
                case "add":
                    if (args.length < 3) {
                        sendUsageMessage(sender);
                        return true;
                    }
                    int secondsToAdd = parsePositiveInteger(args[2], sender);
                    if (secondsToAdd == -1) return true;
                    addFlightTime(sender, target, secondsToAdd);
                    break;

                case "remove":
                    if (args.length < 3) {
                        sendUsageMessage(sender);
                        return true;
                    }
                    int secondsToRemove = parsePositiveInteger(args[2], sender);
                    if (secondsToRemove == -1) return true;
                    removeFlightTime(sender, target, secondsToRemove);
                    break;

                case "set":
                    if (args.length < 3) {
                        sendUsageMessage(sender);
                        return true;
                    }
                    int newFlightTime = parsePositiveInteger(args[2], sender);
                    if (newFlightTime == -1) return true;
                    setFlightTime(sender, target, newFlightTime);
                    break;

                case "clear":
                    clearFlightTime(sender, target);
                    break;

                default:
                    sendUsageMessage(sender);
                    break;
            }
        } catch (Exception e) {
            sender.sendMessage("§cAn error occurred while processing the command.");
            e.printStackTrace();
        }

        return true;
    }

    private void addFlightTime(CommandSender sender, Player player, int secondsToAdd) {
        try {
            int currentFlightTime = databaseHandler.GetPlayerFlightTime(player.getUniqueId());
            int newFlightTime = currentFlightTime + secondsToAdd;

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

            sender.sendMessage("§aRemoved " + secondsToRemove + " seconds of flight time from " + player.getName() + ".");

            String message = this.plugin.getMessagesHandlerInstance().getElytraFlightTimeRemoved().replace("{0}", String.valueOf(secondsToRemove));
            plugin.getMessagesHelper().sendPlayerMessage(player, message);
        } catch (Exception e) {
            sender.sendMessage("§cFailed to remove flight time.");
            e.printStackTrace();
        }
    }

    private void setFlightTime(CommandSender sender, Player player, int newFlightTime) {
        try {
            databaseHandler.SetPlayerFlightTime(player.getUniqueId(), newFlightTime);
            plugin.getElytraFlightListener().UpdatePlayerFlightTime(player.getUniqueId(), newFlightTime);

            sender.sendMessage("§aSet " + player.getName() + "'s flight time to " + newFlightTime + " seconds.");

            String message = this.plugin.getMessagesHandlerInstance().getElytraFlightTimeSet().replace("{0}", String.valueOf(newFlightTime));
            plugin.getMessagesHelper().sendPlayerMessage(player, message);
        } catch (Exception e) {
            sender.sendMessage("§cFailed to set flight time.");
            e.printStackTrace();
        }
    }

    private void clearFlightTime(CommandSender sender, Player player) {
        try {
            databaseHandler.SetPlayerFlightTime(player.getUniqueId(), 0);
            plugin.getElytraFlightListener().UpdatePlayerFlightTime(player.getUniqueId(), 0);

            sender.sendMessage("§aCleared all flight time for " + player.getName() + ".");

            String message = this.plugin.getMessagesHandlerInstance().getElytraFlightTimeCleared();
            plugin.getMessagesHelper().sendPlayerMessage(player, message);
        } catch (Exception e) {
            sender.sendMessage("§cFailed to clear flight time.");
            e.printStackTrace();
        }
    }

    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage("§cUsage:");
        sender.sendMessage("§c/ee flighttime add <player> <seconds>");
        sender.sendMessage("§c/ee flighttime remove <player> <seconds>");
        sender.sendMessage("§c/ee flighttime set <player> <seconds>");
        sender.sendMessage("§c/ee flighttime clear <player>");
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