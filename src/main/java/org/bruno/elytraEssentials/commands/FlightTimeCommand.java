package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlightTimeCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public FlightTimeCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!plugin.getConfigHandlerInstance().getIsTimeLimitEnabled()) {
            sender.sendMessage(ColorHelper.parse(plugin.getMessagesHandlerInstance().getFeatureNotEnabled()));
            return true;
        }

        if (!PermissionsHelper.hasFlightTimeCommandPermission(sender)) {
            sender.sendMessage(ColorHelper.parse(plugin.getMessagesHandlerInstance().getNoPermissionMessage()));
            return true;
        }

        if (args.length < 2) {
            sendUsageMessage(sender);
            return true;
        }

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

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendUsageMessage(sender);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!validateTargetPlayer(sender, target, args[1])) return;

        int amount = parsePositiveInteger(args[2], sender);
        if (amount == -1) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    int currentFlightTime = plugin.getDatabaseHandler().GetPlayerFlightTime(target.getUniqueId());
                    int maxTimeLimit = plugin.getConfigHandlerInstance().getMaxTimeLimit();
                    int finalAmount = amount;
                    int newFlightTime = currentFlightTime + amount;

                    if (maxTimeLimit > 0 && newFlightTime > maxTimeLimit) {
                        finalAmount = maxTimeLimit - currentFlightTime;
                        newFlightTime = maxTimeLimit;
                    }

                    if (finalAmount <= 0) {
                        sender.sendMessage("§cPlayer already has the maximum flight time.");
                        return;
                    }

                    plugin.getDatabaseHandler().SetPlayerFlightTime(target.getUniqueId(), newFlightTime);
                    if (target.isOnline()) {
                        plugin.getElytraFlightListener().setFlightTime(target.getUniqueId(), newFlightTime);
                        String message = plugin.getMessagesHandlerInstance().getElytraFlightTimeAdded().replace("{0}", TimeHelper.formatFlightTime(finalAmount));
                        plugin.getMessagesHelper().sendPlayerMessage(target.getPlayer(), message);
                    }
                    sender.sendMessage("§aAdded " + TimeHelper.formatFlightTime(finalAmount) + " of flight time to " + target.getName() + ".");

                } catch (SQLException e) {
                    handleSqlException(sender, "add flight time to", target.getName(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendUsageMessage(sender);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!validateTargetPlayer(sender, target, args[1])) return;

        int amount = parsePositiveInteger(args[2], sender);
        if (amount == -1) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    int currentFlightTime = plugin.getDatabaseHandler().GetPlayerFlightTime(target.getUniqueId());
                    int newFlightTime = Math.max(0, currentFlightTime - amount);

                    plugin.getDatabaseHandler().SetPlayerFlightTime(target.getUniqueId(), newFlightTime);
                    if (target.isOnline()) {
                        plugin.getElytraFlightListener().setFlightTime(target.getUniqueId(), newFlightTime);
                        String message = plugin.getMessagesHandlerInstance().getElytraFlightTimeRemoved().replace("{0}", TimeHelper.formatFlightTime(amount));
                        plugin.getMessagesHelper().sendPlayerMessage(target.getPlayer(), message);
                    }
                    sender.sendMessage("§aRemoved " + TimeHelper.formatFlightTime(amount) + " of flight time from " + target.getName() + ".");

                } catch (SQLException e) {
                    handleSqlException(sender, "remove flight time from", target.getName(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendUsageMessage(sender);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!validateTargetPlayer(sender, target, args[1])) return;

        int amount = parsePositiveInteger(args[2], sender);
        if (amount == -1) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    int maxTimeLimit = plugin.getConfigHandlerInstance().getMaxTimeLimit();
                    int finalAmount = (maxTimeLimit > 0) ? Math.min(amount, maxTimeLimit) : amount;

                    plugin.getDatabaseHandler().SetPlayerFlightTime(target.getUniqueId(), finalAmount);
                    if (target.isOnline()) {
                        plugin.getElytraFlightListener().setFlightTime(target.getUniqueId(), finalAmount);
                        String message = plugin.getMessagesHandlerInstance().getElytraFlightTimeSet().replace("{0}", TimeHelper.formatFlightTime(finalAmount));
                        plugin.getMessagesHelper().sendPlayerMessage(target.getPlayer(), message);
                    }
                    sender.sendMessage("§aSet " + target.getName() + "'s flight time to " + TimeHelper.formatFlightTime(finalAmount));

                } catch (SQLException e) {
                    handleSqlException(sender, "set flight time for", target.getName(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsageMessage(sender);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!validateTargetPlayer(sender, target, args[1])) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getDatabaseHandler().SetPlayerFlightTime(target.getUniqueId(), 0);
                    if (target.isOnline()) {
                        plugin.getElytraFlightListener().setFlightTime(target.getUniqueId(), 0);
                        plugin.getMessagesHelper().sendPlayerMessage(target.getPlayer(), plugin.getMessagesHandlerInstance().getElytraFlightTimeCleared());
                    }
                    sender.sendMessage("§aCleared all flight time for " + target.getName() + ".");
                } catch (SQLException e) {
                    handleSqlException(sender, "clear flight time for", target.getName(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private boolean validateTargetPlayer(CommandSender sender, OfflinePlayer target, String targetName) {
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            String message = plugin.getMessagesHandlerInstance().getPlayerNotFound().replace("{0}", targetName);
            sender.sendMessage(ColorHelper.parse(message));
            return false;
        }
        if (target.isOnline() && PermissionsHelper.PlayerBypassTimeLimit(target.getPlayer())) {
            sender.sendMessage("§cThe player " + targetName + " has time limit bypass and cannot be managed.");
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
            sender.sendMessage("§cInvalid number. Please enter a positive integer.");
            return -1;
        }
    }

    private void handleSqlException(CommandSender sender, String action, String targetName, SQLException e) {
        plugin.getLogger().log(Level.SEVERE, "Failed to " + action + " " + targetName, e);
        sender.sendMessage(ChatColor.RED + "A database error occurred. Please check the console for details.");
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
}