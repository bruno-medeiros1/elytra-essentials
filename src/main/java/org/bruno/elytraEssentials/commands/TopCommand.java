package org.bruno.elytraEssentials.commands;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.handlers.StatsHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TopCommand implements ISubCommand {
    private final StatsHandler statsHandler;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler;

    public TopCommand(StatsHandler statsHandler, MessagesHelper messagesHelper, MessagesHandler messagesHandler) {
        this.statsHandler = statsHandler;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !PermissionsHelper.hasTopPermission(player)) {
            messagesHelper.sendPlayerMessage(player, messagesHandler.getNoPermissionMessage());
            return true;
        }

        if (args.length == 0) {
            sendLeaderboardMenu(sender);
            return true;
        }

        if (args.length > 1){
            messagesHelper.sendCommandSenderMessage(sender,"&cUsage: /ee top <distance|time|longest>");
            return true;
        }

        // Delegate the complex work to the handler
        statsHandler.displayTopStats(sender, args[0].toLowerCase());
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

        sendLeaderboardLine(player, "§eTotal Distance Flown", "/ee top distance");
        sendLeaderboardLine(player, "§eTotal Flight Time", "/ee top time");
        sendLeaderboardLine(player, "§eLongest Single Flight", "/ee top longest");

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
