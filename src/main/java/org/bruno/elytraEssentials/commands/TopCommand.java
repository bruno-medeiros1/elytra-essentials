package org.bruno.elytraEssentials.commands;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.helpers.TimeHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TopCommand implements ISubCommand {
    private final ElytraEssentials plugin;
    private static final int LEADERBOARD_LIMIT = 5;

    public TopCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
            plugin.getMessagesHelper().sendCommandSenderMessage(sender,"&cThis command can only be run by players or the console.");
            return true;
        }

        if (args.length > 1){
            plugin.getMessagesHelper().sendCommandSenderMessage(sender,"&cUsage: /ee top <distance,time,longest>");
            return true;
        }

        if (sender instanceof Player player){
            if (!PermissionsHelper.hasTopPermission(player)){
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
                return true;
            }
        }


        if (args.length < 1) {
            sendLeaderboardMenu(sender);
            return true;
        }

        String category = args[0].toLowerCase();
        String dbColumn;
        String title;
        String format;

        // Determine which statistic to query based on the argument
        switch (category) {
            case "distance":
                dbColumn = "total_distance";
                title = "Top Distance Flown";
                format = "§e#%d §f%s §7- §b%.1f km";
                break;
            case "time":
                dbColumn = "total_time_seconds";
                title = "Top Flight Time";
                format = "§e#%d §f%s §7- §b%s";
                break;
            case "longest":
                dbColumn = "longest_flight";
                title = "Longest Single Flights";
                format = "§e#%d §f%s §7- §b%.0f blocks";
                break;
            default:
                plugin.getMessagesHelper().sendCommandSenderMessage(sender,"&cInvalid category. Use: distance, time, longest");
                return true;
        }

        // Run the database query asynchronously to prevent server lag
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Map<UUID, Double> topData = plugin.getDatabaseHandler().getTopStats(dbColumn, LEADERBOARD_LIMIT);
                    List<String> formattedMessages = new ArrayList<>();

                    if (topData.isEmpty()) {
                        formattedMessages.add(ChatColor.GRAY + "No statistics available for this category yet.");
                    } else {
                        int rank = 1;
                        for (Map.Entry<UUID, Double> entry : topData.entrySet()) {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                            String playerName = (player.getName() != null) ? player.getName() : "Unknown";
                            Double value = entry.getValue();

                            String formattedLine = "";
                            switch (category) {
                                case "time" ->
                                        formattedLine = String.format(format, rank, playerName, TimeHelper.formatFlightTime(value.intValue()));
                                case "distance" ->
                                        formattedLine = String.format(format, rank, playerName, value / 1000.0);
                                case "longest" -> formattedLine = String.format(format, rank, playerName, value);
                            }
                            if (!formattedLine.isBlank()) {
                                formattedMessages.add(formattedLine);
                                rank++;
                            }
                        }
                    }

                    // Send the formatted messages back to the player on the main server thread
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage("§6--- " + title + " ---");
                            for (String message : formattedMessages) {
                                sender.sendMessage(message);
                            }
                            sender.sendMessage("§6-------------------------------------");
                        }
                    }.runTask(plugin);

                } catch (SQLException e) {
                    plugin.getMessagesHelper().sendCommandSenderMessage(sender,"&cAn error occurred while fetching the leaderboard.");
                    plugin.getLogger().log(Level.SEVERE, "An error occurred while fetching the leaderboard.", e);
                }
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }

    private void sendLeaderboardMenu(CommandSender sender) {
        // The sender must be a player to receive interactive components.
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§6--- ElytraEssentials Leaderboards ---");
            sender.sendMessage("§eUse /ee top <category> to view a leaderboard.");
            sender.sendMessage("§7Categories: distance, time, longest");
            return;
        }

        player.sendMessage("§6§m----------------------------------------------------");
        player.sendMessage("");
        player.sendMessage("§6§lElytraEssentials Leaderboards");
        player.sendMessage("§7Click a category to view the top players.");
        player.sendMessage("");

        sendLeaderboardLine(player, "§7Total Distance Flown", "/ee top distance");
        sendLeaderboardLine(player, "§7Total Flight Time", "/ee top time");
        sendLeaderboardLine(player, "§7Longest Single Flight", "/ee top longest");

        player.sendMessage("");
        player.sendMessage("§6§m----------------------------------------------------");
    }

    /**
     * Helper method to build and send a single clickable line for the menu.
     */
    private void sendLeaderboardLine(Player player, String categoryName, String commandToRun) {
        TextComponent message = new TextComponent(TextComponent.fromLegacyText("§e» "));

        TextComponent categoryComponent = new TextComponent(TextComponent.fromLegacyText(categoryName));
        categoryComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandToRun));

        // Create the hover text
        BaseComponent[] hoverText = new TextComponent[]{
                new TextComponent(TextComponent.fromLegacyText("§fClick to view the\n")),
                new TextComponent(TextComponent.fromLegacyText("§e" + categoryName)),
                new TextComponent(TextComponent.fromLegacyText("\n§fleaderboard."))
        };
        categoryComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

        message.addExtra(categoryComponent);
        player.spigot().sendMessage(message);
    }


    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
            return List.of();
        }

        if (args.length == 2) {
            if (sender instanceof Player player) {
                if (!PermissionsHelper.hasTopPermission(player))
                    return List.of();
            }

            // A list of all possible leaderboard categories
            List<String> allCategories = List.of("distance", "time", "longest");

            // Check permission for each category before adding it as a suggestion
            List<String> allowedCategories = new ArrayList<>(allCategories);

            // Now, filter the list of *allowed* categories based on what the player is typing
            String currentArg = args[1].toLowerCase();
            return allowedCategories.stream()
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
