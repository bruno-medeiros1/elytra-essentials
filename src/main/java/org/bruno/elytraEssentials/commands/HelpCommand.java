package org.bruno.elytraEssentials.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HelpCommand implements ISubCommand {

    private final ElytraEssentials plugin;
    private final List<HelpEntry> allCommands = new ArrayList<>();
    private static final int COMMANDS_PER_PAGE = 5;

    // A private record to hold all info about a help entry
    private record HelpEntry(String command, String description, String permission) {}

    public HelpCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
        initializeHelpEntries();
    }

    private void initializeHelpEntries() {
        allCommands.add(new HelpEntry("/ee reload", "Reloads the plugin's configuration files.", Constants.Permissions.CMD_RELOAD));
        allCommands.add(new HelpEntry("/ee shop", "Opens the effects shop GUI.", Constants.Permissions.CMD_SHOP));

        allCommands.add(new HelpEntry("/ee effects", "Opens the owned effects GUI.", Constants.Permissions.CMD_EFFECTS));
        allCommands.add(new HelpEntry("/ee effects clear", "Clears your active elytra effect.", Constants.Permissions.CMD_EFFECTS_CLEAR));
        allCommands.add(new HelpEntry("/ee effects give <player> <effect>", "Gives an effect to a player.", Constants.Permissions.CMD_EFFECTS_GIVE));
        allCommands.add(new HelpEntry("/ee effects remove <player> <effect>", "Removes an effect from a player.", Constants.Permissions.CMD_EFFECTS_REMOVE));
        allCommands.add(new HelpEntry("/ee effects list <player>", "Shows the player's owned effects.", Constants.Permissions.CMD_EFFECTS_LIST));

        allCommands.add(new HelpEntry("/ee ft set <player> <seconds>", "Sets the total player's flight time.", Constants.Permissions.CMD_FLIGHT_TIME));
        allCommands.add(new HelpEntry("/ee ft add <player> <seconds>", "Adds time to the player's flight time.", Constants.Permissions.CMD_FLIGHT_TIME));
        allCommands.add(new HelpEntry("/ee ft remove <player> <seconds>", "Removes time from the player's flight time.", Constants.Permissions.CMD_FLIGHT_TIME));
        allCommands.add(new HelpEntry("/ee ft clear <player>", "Clears all flight time a player has.", Constants.Permissions.CMD_FLIGHT_TIME));

        allCommands.add(new HelpEntry("/ee stats", "View your flight statistics.", Constants.Permissions.CMD_STATS));
        allCommands.add(new HelpEntry("/ee stats <player>", "View other player's flight statistics.", Constants.Permissions.CMD_STATS_OTHERS));

        allCommands.add(new HelpEntry("/ee top <distance, time, longest>", "View leaderboard statistics.", Constants.Permissions.CMD_TOP));

        allCommands.add(new HelpEntry("/ee forge", "Opens the Armored Elytra forge GUI.", Constants.Permissions.CMD_FORGE));
        allCommands.add(new HelpEntry("/ee armor", "Shows stats for your worn Armored Elytra.", Constants.Permissions.CMD_ARMOR));
        allCommands.add(new HelpEntry("/ee armor repair", "Repairs your worn Armored Elytra.", Constants.Permissions.CMD_REPAIR));

        allCommands.add(new HelpEntry("/ee importdb <file> --confirm", "Restores a database backup.", Constants.Permissions.CMD_IMPORT_DB));

        allCommands.add(new HelpEntry("/ee achievements", "Opens the achievements GUI.", Constants.Permissions.CMD_ACHIEVEMENTS));
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Filter commands based on sender's permissions
        List<HelpEntry> allowedCommands = allCommands.stream()
                .filter(entry -> sender.hasPermission(entry.permission()))
                .toList();

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) allowedCommands.size() / COMMANDS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        int page = 1;
        if (args.length == 1) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1 || page > totalPages) {
                    sender.sendMessage("§cInvalid page number.");
                    return true;
                }
                if (sender instanceof Player)
                    ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);

            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid page number. Please use a number.");
                return true;
            }
        }

        // Send the help header and the list of commands
        sendHeader(sender);

        int startIndex = (page - 1) * COMMANDS_PER_PAGE;
        for (int i = 0; i < COMMANDS_PER_PAGE; i++) {
            int listIndex = startIndex + i;
            if (listIndex < allowedCommands.size()) {
                sendHelpLine(sender, allowedCommands.get(listIndex));
            }
        }

        sendWebsiteLink(sender);
        sendNavigationFooter(sender, page, totalPages);
        return true;
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Calculate how many pages are available for this specific sender
            long allowedCount = allCommands.stream().filter(entry -> sender.hasPermission(entry.permission())).count();
            int totalPages = (int) Math.ceil((double) allowedCount / COMMANDS_PER_PAGE);

            // Generate a list of page numbers as strings
            return IntStream.rangeClosed(1, totalPages)
                    .mapToObj(String::valueOf)
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void sendHeader(CommandSender sender) {
        String primary = "§6";
        String secondary = "§e";
        String text = "§7";
        sender.sendMessage(primary + "§m----------------------------------------------------");
        sender.sendMessage("");
        sender.sendMessage(primary + "§lElytraEssentials " + secondary + "v" + plugin.getDescription().getVersion());
        sender.sendMessage(text + "All available commands are listed below.");
        sender.sendMessage("");
    }

    private void sendHelpLine(CommandSender sender, HelpEntry entry) {
        TextComponent message = new TextComponent(TextComponent.fromLegacyText("§e» "));

        TextComponent commandComponent = new TextComponent(TextComponent.fromLegacyText("§e" + entry.command()));
        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, entry.command()));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(TextComponent.fromLegacyText("§eClick to paste command in the chat.\n§7Permission: §f" + entry.permission()))
        ));

        message.addExtra(commandComponent);
        message.addExtra(new TextComponent(TextComponent.fromLegacyText(" §7- " + entry.description())));

        sender.spigot().sendMessage(message);
    }

    private void sendNavigationFooter(CommandSender sender, int currentPage, int totalPages) {
        sender.sendMessage("");

        TextComponent footer = new TextComponent("§6§m------------§r§6[ §e");

        // Previous Page Button
        if (currentPage > 1) {
            TextComponent prevButton = new TextComponent(TextComponent.fromLegacyText("« Prev"));
            prevButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ee help " + (currentPage - 1)));
            prevButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Go to previous page")));
            footer.addExtra(prevButton);
        } else {
            footer.addExtra(new TextComponent("§7§m« Prev"));
        }

        footer.addExtra(new TextComponent(String.format(" §r§6| §fPage %d/%d §6|§r ", currentPage, totalPages)));

        // Next Page Button
        if (currentPage < totalPages) {
            TextComponent nextButton = new TextComponent(TextComponent.fromLegacyText("Next »"));
            nextButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ee help " + (currentPage + 1)));
            nextButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Go to next page")));
            footer.addExtra(nextButton);
        } else {
            footer.addExtra(new TextComponent("§7§mNext »"));
        }

        footer.addExtra(new TextComponent(" §r§6]§m------------"));
        sender.spigot().sendMessage(footer);
    }

    private void sendWebsiteLink(CommandSender sender) {
        sender.sendMessage("");

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§6For more information, visit: https://www.spigotmc.org/resources/126002/");
            return;
        }

        TextComponent message = new TextComponent(TextComponent.fromLegacyText("§6For more information, visit our "));

        TextComponent linkComponent = new TextComponent("§e§n[Official Plugin Page]");
        linkComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/126002/"));
        linkComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open the website!")));

        message.addExtra(linkComponent);
        player.spigot().sendMessage(message);
    }
}
